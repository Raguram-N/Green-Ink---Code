package com.greenink.api.infrastructure.jooq;

import com.greenink.api.content.ContentRepository;
import com.greenink.api.content.NoteDocument;
import org.jooq.DSLContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.greenink.jooq.generated.tables.SyllabusUnits.SYLLABUS_UNITS;
import static com.greenink.jooq.generated.tables.Topics.TOPICS;
import static com.greenink.jooq.generated.tables.UnitSyllabusContent.UNIT_SYLLABUS_CONTENT;

@Repository
@Profile("jooq")
public class JooqContentRepository implements ContentRepository {

    private static final Pattern CHAPTER_ID =
            Pattern.compile("^u(\\d+)-c(\\d+)$");

    private final DSLContext dsl;

    public JooqContentRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    @Override
    public Optional<NoteDocument> findNotesByChapterId(
            String chapterId,
            String language
    ) {
        int[] numbers = parseChapterNumbers(chapterId);

        if (numbers == null) {
            return Optional.empty();
        }

        int unitNumber = numbers[0];
        int chapterNumber = numbers[1];

        var rows = dsl
                .select(
                        UNIT_SYLLABUS_CONTENT.PAGE_NUMBER,
                        UNIT_SYLLABUS_CONTENT.CONTENT_PRIMARY,
                        UNIT_SYLLABUS_CONTENT.CONTENT_SECONDARY,
                        UNIT_SYLLABUS_CONTENT.UPDATED_AT
                )
                .from(UNIT_SYLLABUS_CONTENT)
                .join(SYLLABUS_UNITS)
                .on(SYLLABUS_UNITS.ID.eq(
                        UNIT_SYLLABUS_CONTENT.UNIT_ID
                ))
                .join(TOPICS)
                .on(TOPICS.ID.eq(
                        SYLLABUS_UNITS.TOPIC_ID
                ))
                .where(TOPICS.DISPLAY_ORDER.eq(unitNumber))
                .and(SYLLABUS_UNITS.DISPLAY_ORDER.eq(chapterNumber))
                .and(TOPICS.IS_ACTIVE.eq(true))
                .and(SYLLABUS_UNITS.IS_ACTIVE.eq(true))
                .and(SYLLABUS_UNITS.NOTES_AVAILABLE.eq(true))
                .and(UNIT_SYLLABUS_CONTENT.IS_PUBLISHED.eq(true))
                .orderBy(UNIT_SYLLABUS_CONTENT.PAGE_NUMBER)
                .fetch();

        if (rows.isEmpty()) {
            return Optional.empty();
        }

        boolean tamil = "ta".equalsIgnoreCase(language);

        StringBuilder body = new StringBuilder();
        OffsetDateTime latestUpdatedAt = null;

        for (var row : rows) {
            String content;

            if (tamil) {
                content = row.get(
                        UNIT_SYLLABUS_CONTENT.CONTENT_PRIMARY
                );
            } else {
                content = row.get(
                        UNIT_SYLLABUS_CONTENT.CONTENT_SECONDARY
                );

                if (content == null || content.isBlank()) {
                    content = row.get(
                            UNIT_SYLLABUS_CONTENT.CONTENT_PRIMARY
                    );
                }
            }

            if (content != null && !content.isBlank()) {
                if (!body.isEmpty()) {
                    body.append(System.lineSeparator());
                }

                body.append(content);
            }

            OffsetDateTime updatedAt =
                    row.get(UNIT_SYLLABUS_CONTENT.UPDATED_AT);

            if (updatedAt != null
                    && (latestUpdatedAt == null
                    || updatedAt.isAfter(latestUpdatedAt))) {
                latestUpdatedAt = updatedAt;
            }
        }

        if (body.isEmpty()) {
            return Optional.empty();
        }

        String version = latestUpdatedAt == null
                ? "db-1"
                : "db-" + latestUpdatedAt
                        .toInstant()
                        .toEpochMilli();

        return Optional.of(
                new NoteDocument(
                        chapterId,
                        version,
                        "HTML_FRAGMENT",
                        body.toString()
                )
        );
    }

    private int[] parseChapterNumbers(String chapterId) {
        if (chapterId == null) {
            return null;
        }

        Matcher matcher = CHAPTER_ID.matcher(chapterId);

        if (!matcher.matches()) {
            return null;
        }

        return new int[] {
                Integer.parseInt(matcher.group(1)),
                Integer.parseInt(matcher.group(2))
        };
    }
}