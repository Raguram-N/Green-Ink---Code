package com.greenink.api.infrastructure.jooq;

import com.greenink.api.progress.ProgressRepository;
import org.jooq.DSLContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.greenink.jooq.generated.tables.NotesProgress.NOTES_PROGRESS;
import static com.greenink.jooq.generated.tables.SyllabusUnits.SYLLABUS_UNITS;
import static com.greenink.jooq.generated.tables.Topics.TOPICS;

@Repository
@Profile("jooq")
public class JooqProgressRepository implements ProgressRepository {

    private static final Pattern CHAPTER_ID =
            Pattern.compile("^u(\\d+)-c(\\d+)$");

    private final DSLContext dsl;

    /*
     * The locked schema has NOTES_PROGRESS for note completion.
     *
     * It does not have a chapter-level PYQ progress aggregate matching
     * ProgressRepository.PyqChapterProgress, so that aggregate remains
     * application-memory state until a schema-supported persistence
     * strategy is defined.
     */
    private final ConcurrentHashMap<
            String,
            ConcurrentHashMap<String, PyqChapterProgress>
            > pyqByUser = new ConcurrentHashMap<>();

    public JooqProgressRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    @Override
    public boolean isNotesCompleted(
            String userId,
            String chapterId
    ) {
        Long databaseUserId = parseUserId(userId);
        Long syllabusUnitId = findSyllabusUnitId(chapterId);

        if (databaseUserId == null || syllabusUnitId == null) {
            return false;
        }

        Boolean completed = dsl
                .select(NOTES_PROGRESS.COMPLETED)
                .from(NOTES_PROGRESS)
                .where(
                        NOTES_PROGRESS.USER_ID.eq(
                                databaseUserId
                        )
                )
                .and(
                        NOTES_PROGRESS.UNIT_ID.eq(
                                syllabusUnitId
                        )
                )
                .fetchOne(NOTES_PROGRESS.COMPLETED);

        return Boolean.TRUE.equals(completed);
    }

    @Override
    public void setNotesCompleted(
            String userId,
            String chapterId,
            boolean completed
    ) {
        Long databaseUserId = requireUserId(userId);
        Long syllabusUnitId =
                requireSyllabusUnitId(chapterId);

        int updated = dsl
                .update(NOTES_PROGRESS)
                .set(NOTES_PROGRESS.COMPLETED, completed)
                .set(
                        NOTES_PROGRESS.SCROLL_PERCENT,
                        completed ? 100 : 0
                )
                .set(
                        NOTES_PROGRESS.UPDATED_AT,
                        java.time.OffsetDateTime.now()
                )
                .where(
                        NOTES_PROGRESS.USER_ID.eq(
                                databaseUserId
                        )
                )
                .and(
                        NOTES_PROGRESS.UNIT_ID.eq(
                                syllabusUnitId
                        )
                )
                .execute();

        if (updated == 0) {
            dsl.insertInto(NOTES_PROGRESS)
                    .set(
                            NOTES_PROGRESS.USER_ID,
                            databaseUserId
                    )
                    .set(
                            NOTES_PROGRESS.UNIT_ID,
                            syllabusUnitId
                    )
                    .set(
                            NOTES_PROGRESS.COMPLETED,
                            completed
                    )
                    .set(
                            NOTES_PROGRESS.SCROLL_PERCENT,
                            completed ? 100 : 0
                    )
                    .execute();
        }
    }

    @Override
    public Set<String> notesCompletedChapterIds(
            String userId
    ) {
        Long databaseUserId = parseUserId(userId);

        if (databaseUserId == null) {
            return Set.of();
        }

        return dsl
                .select(
                        TOPICS.DISPLAY_ORDER,
                        SYLLABUS_UNITS.DISPLAY_ORDER
                )
                .from(NOTES_PROGRESS)
                .join(SYLLABUS_UNITS)
                .on(
                        SYLLABUS_UNITS.ID.eq(
                                NOTES_PROGRESS.UNIT_ID
                        )
                )
                .join(TOPICS)
                .on(
                        TOPICS.ID.eq(
                                SYLLABUS_UNITS.TOPIC_ID
                        )
                )
                .where(
                        NOTES_PROGRESS.USER_ID.eq(
                                databaseUserId
                        )
                )
                .and(NOTES_PROGRESS.COMPLETED.eq(true))
                .and(TOPICS.IS_ACTIVE.eq(true))
                .and(SYLLABUS_UNITS.IS_ACTIVE.eq(true))
                .fetch()
                .stream()
                .map(record ->
                        "u"
                                + record.get(
                                        TOPICS.DISPLAY_ORDER
                                )
                                + "-c"
                                + record.get(
                                        SYLLABUS_UNITS.DISPLAY_ORDER
                                )
                )
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public void recordPyqAttempt(
            String userId,
            String chapterId,
            int answered,
            int correct
    ) {
        var byChapter = pyqByUser.computeIfAbsent(
                userId,
                ignored -> new ConcurrentHashMap<>()
        );

        byChapter.merge(
                chapterId,
                new PyqChapterProgress(answered, correct),
                (oldValue, newValue) ->
                        new PyqChapterProgress(
                                Math.max(
                                        oldValue.answered(),
                                        newValue.answered()
                                ),
                                Math.max(
                                        oldValue.bestCorrect(),
                                        newValue.bestCorrect()
                                )
                        )
        );
    }

    @Override
    public PyqProgressSnapshot pyqSnapshot(
            String userId
    ) {
        Map<String, PyqChapterProgress> existing =
                pyqByUser.get(userId);

        if (existing == null) {
            return new PyqProgressSnapshot(Map.of());
        }

        return new PyqProgressSnapshot(
                Map.copyOf(existing)
        );
    }

    @Override
    public void resetNotes(String userId) {
        Long databaseUserId = parseUserId(userId);

        if (databaseUserId == null) {
            return;
        }

        dsl.deleteFrom(NOTES_PROGRESS)
                .where(
                        NOTES_PROGRESS.USER_ID.eq(
                                databaseUserId
                        )
                )
                .execute();
    }

    @Override
    public void resetPyq(String userId) {
        pyqByUser.remove(userId);
    }

    @Override
    public void deleteUser(String userId) {
        resetNotes(userId);
        resetPyq(userId);
    }

    private Long findSyllabusUnitId(
            String chapterId
    ) {
        int[] numbers = parseChapterNumbers(chapterId);

        if (numbers == null) {
            return null;
        }

        return dsl
                .select(SYLLABUS_UNITS.ID)
                .from(SYLLABUS_UNITS)
                .join(TOPICS)
                .on(
                        TOPICS.ID.eq(
                                SYLLABUS_UNITS.TOPIC_ID
                        )
                )
                .where(
                        TOPICS.DISPLAY_ORDER.eq(numbers[0])
                )
                .and(
                        SYLLABUS_UNITS.DISPLAY_ORDER.eq(
                                numbers[1]
                        )
                )
                .and(TOPICS.IS_ACTIVE.eq(true))
                .and(SYLLABUS_UNITS.IS_ACTIVE.eq(true))
                .fetchOne(SYLLABUS_UNITS.ID);
    }

    private Long requireSyllabusUnitId(
            String chapterId
    ) {
        Long id = findSyllabusUnitId(chapterId);

        if (id == null) {
            throw new IllegalArgumentException(
                    "Invalid chapter ID: " + chapterId
            );
        }

        return id;
    }

    private Long parseUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            return null;
        }

        try {
            return Long.valueOf(userId);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Long requireUserId(String userId) {
        Long id = parseUserId(userId);

        if (id == null) {
            throw new IllegalArgumentException(
                    "Invalid database user ID: " + userId
            );
        }

        return id;
    }

    private int[] parseChapterNumbers(
            String chapterId
    ) {
        if (chapterId == null) {
            return null;
        }

        Matcher matcher =
                CHAPTER_ID.matcher(chapterId);

        if (!matcher.matches()) {
            return null;
        }

        return new int[] {
                Integer.parseInt(matcher.group(1)),
                Integer.parseInt(matcher.group(2))
        };
    }
}