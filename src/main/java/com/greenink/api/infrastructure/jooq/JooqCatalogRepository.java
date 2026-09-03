package com.greenink.api.infrastructure.jooq;

import com.greenink.api.catalog.AccessTier;
import com.greenink.api.catalog.CatalogRepository;
import com.greenink.api.catalog.ChapterDefinition;
import com.greenink.api.catalog.UnitDefinition;
import com.greenink.jooq.generated.tables.records.SyllabusUnitsRecord;
import com.greenink.jooq.generated.tables.records.TopicsRecord;
import org.jooq.DSLContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.greenink.jooq.generated.tables.SyllabusUnits.SYLLABUS_UNITS;
import static com.greenink.jooq.generated.tables.Topics.TOPICS;

@Repository
@Profile("jooq")
public class JooqCatalogRepository implements CatalogRepository {

    private static final Pattern UNIT_ID = Pattern.compile("^u(\\d+)$");
    private static final Pattern CHAPTER_ID = Pattern.compile("^u(\\d+)-c(\\d+)$");

    private final DSLContext dsl;

    public JooqCatalogRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    @Override
    public List<UnitDefinition> findAllUnits() {
        List<TopicsRecord> topics = dsl
                .selectFrom(TOPICS)
                .where(TOPICS.IS_ACTIVE.eq(true))
                .orderBy(TOPICS.DISPLAY_ORDER)
                .fetch();

        List<SyllabusUnitsRecord> chapters = dsl
                .selectFrom(SYLLABUS_UNITS)
                .where(SYLLABUS_UNITS.IS_ACTIVE.eq(true))
                .orderBy(SYLLABUS_UNITS.TOPIC_ID, SYLLABUS_UNITS.DISPLAY_ORDER)
                .fetch();

        Map<Long, List<ChapterDefinition>> chaptersByTopic = new LinkedHashMap<>();

        for (SyllabusUnitsRecord chapter : chapters) {
            Long topicId = chapter.get(SYLLABUS_UNITS.TOPIC_ID);

            TopicsRecord topic = topics.stream()
                    .filter(t -> t.get(TOPICS.ID).equals(topicId))
                    .findFirst()
                    .orElse(null);

            if (topic == null) {
                continue;
            }

            chaptersByTopic
                    .computeIfAbsent(topicId, ignored -> new ArrayList<>())
                    .add(toChapter(
                            topic.get(TOPICS.DISPLAY_ORDER),
                            chapter
                    ));
        }

        return topics.stream()
                .map(topic -> toUnit(
                        topic,
                        chaptersByTopic.getOrDefault(
                                topic.get(TOPICS.ID),
                                List.of()
                        )
                ))
                .toList();
    }

    @Override
    public Optional<UnitDefinition> findUnit(String unitId) {
        Integer unitNumber = parseUnitNumber(unitId);

        if (unitNumber == null) {
            return Optional.empty();
        }

        TopicsRecord topic = dsl
                .selectFrom(TOPICS)
                .where(TOPICS.DISPLAY_ORDER.eq(unitNumber))
                .and(TOPICS.IS_ACTIVE.eq(true))
                .fetchOne();

        if (topic == null) {
            return Optional.empty();
        }

        List<ChapterDefinition> chapters = dsl
                .selectFrom(SYLLABUS_UNITS)
                .where(SYLLABUS_UNITS.TOPIC_ID.eq(topic.get(TOPICS.ID)))
                .and(SYLLABUS_UNITS.IS_ACTIVE.eq(true))
                .orderBy(SYLLABUS_UNITS.DISPLAY_ORDER)
                .fetch()
                .map(record -> toChapter(unitNumber, record));

        return Optional.of(toUnit(topic, chapters));
    }

    @Override
    public Optional<ChapterDefinition> findChapter(String chapterId) {
        int[] numbers = parseChapterNumbers(chapterId);

        if (numbers == null) {
            return Optional.empty();
        }

        int unitNumber = numbers[0];
        int chapterNumber = numbers[1];

        Long topicId = dsl
                .select(TOPICS.ID)
                .from(TOPICS)
                .where(TOPICS.DISPLAY_ORDER.eq(unitNumber))
                .and(TOPICS.IS_ACTIVE.eq(true))
                .fetchOne(TOPICS.ID);

        if (topicId == null) {
            return Optional.empty();
        }

        SyllabusUnitsRecord chapter = dsl
                .selectFrom(SYLLABUS_UNITS)
                .where(SYLLABUS_UNITS.TOPIC_ID.eq(topicId))
                .and(SYLLABUS_UNITS.DISPLAY_ORDER.eq(chapterNumber))
                .and(SYLLABUS_UNITS.IS_ACTIVE.eq(true))
                .fetchOne();

        return Optional.ofNullable(chapter)
                .map(record -> toChapter(unitNumber, record));
    }

    @Override
    public Optional<UnitDefinition> findUnitByChapterId(String chapterId) {
        int[] numbers = parseChapterNumbers(chapterId);

        if (numbers == null || findChapter(chapterId).isEmpty()) {
            return Optional.empty();
        }

        return findUnit("u" + numbers[0]);
    }

    @Override
    public List<ChapterDefinition> searchChapters(String query, int limit) {
        if (query == null || query.isBlank() || limit <= 0) {
            return List.of();
        }

        String q = query.trim();

        return dsl
                .select(
                        TOPICS.DISPLAY_ORDER,
                        SYLLABUS_UNITS.DISPLAY_ORDER,
                        SYLLABUS_UNITS.TITLE_PRIMARY,
                        SYLLABUS_UNITS.TITLE_SECONDARY,
                        SYLLABUS_UNITS.REQUIRED_TIER_LEVEL,
                        SYLLABUS_UNITS.NOTES_AVAILABLE
                )
                .from(SYLLABUS_UNITS)
                .join(TOPICS)
                .on(TOPICS.ID.eq(SYLLABUS_UNITS.TOPIC_ID))
                .where(TOPICS.IS_ACTIVE.eq(true))
                .and(SYLLABUS_UNITS.IS_ACTIVE.eq(true))
                .and(
                        SYLLABUS_UNITS.TITLE_PRIMARY.containsIgnoreCase(q)
                                .or(SYLLABUS_UNITS.TITLE_SECONDARY.containsIgnoreCase(q))
                )
                .orderBy(
                        TOPICS.DISPLAY_ORDER,
                        SYLLABUS_UNITS.DISPLAY_ORDER
                )
                .limit(limit)
                .fetch(record -> new ChapterDefinition(
                        chapterId(
                                record.get(TOPICS.DISPLAY_ORDER),
                                record.get(SYLLABUS_UNITS.DISPLAY_ORDER)
                        ),
                        record.get(SYLLABUS_UNITS.DISPLAY_ORDER),
                        preferredTitle(
                                record.get(SYLLABUS_UNITS.TITLE_PRIMARY),
                                record.get(SYLLABUS_UNITS.TITLE_SECONDARY)
                        ),
                        toAccessTier(
                                record.get(SYLLABUS_UNITS.REQUIRED_TIER_LEVEL)
                        ),
                        Boolean.TRUE.equals(
                                record.get(SYLLABUS_UNITS.NOTES_AVAILABLE)
                        )
                ));
    }

    @Override
    public int totalChapterCount() {
        Integer count = dsl
                .selectCount()
                .from(SYLLABUS_UNITS)
                .join(TOPICS)
                .on(TOPICS.ID.eq(SYLLABUS_UNITS.TOPIC_ID))
                .where(TOPICS.IS_ACTIVE.eq(true))
                .and(SYLLABUS_UNITS.IS_ACTIVE.eq(true))
                .fetchOne(0, Integer.class);

        return count == null ? 0 : count;
    }

    private UnitDefinition toUnit(
            TopicsRecord topic,
            List<ChapterDefinition> chapters
    ) {
        int number = topic.get(TOPICS.DISPLAY_ORDER);

        return new UnitDefinition(
                unitId(number),
                number,
                toRoman(number),
                preferredTitle(
                        topic.get(TOPICS.TITLE_PRIMARY),
                        topic.get(TOPICS.TITLE_SECONDARY)
                ),
                List.copyOf(chapters)
        );
    }

    private ChapterDefinition toChapter(
            int unitNumber,
            SyllabusUnitsRecord chapter
    ) {
        int chapterNumber = chapter.get(SYLLABUS_UNITS.DISPLAY_ORDER);

        return new ChapterDefinition(
                chapterId(unitNumber, chapterNumber),
                chapterNumber,
                preferredTitle(
                        chapter.get(SYLLABUS_UNITS.TITLE_PRIMARY),
                        chapter.get(SYLLABUS_UNITS.TITLE_SECONDARY)
                ),
                toAccessTier(
                        chapter.get(SYLLABUS_UNITS.REQUIRED_TIER_LEVEL)
                ),
                Boolean.TRUE.equals(
                        chapter.get(SYLLABUS_UNITS.NOTES_AVAILABLE)
                )
        );
    }

    private AccessTier toAccessTier(Integer tierLevel) {
        return tierLevel == null || tierLevel <= 0
                ? AccessTier.FREE
                : AccessTier.PREMIUM;
    }

    private String preferredTitle(
            String primary,
            String secondary
    ) {
        if (secondary != null && !secondary.isBlank()) {
            return secondary;
        }

        return primary;
    }

    private String unitId(int unitNumber) {
        return "u" + unitNumber;
    }

    private String chapterId(
            int unitNumber,
            int chapterNumber
    ) {
        return unitId(unitNumber) + "-c" + chapterNumber;
    }

    private Integer parseUnitNumber(String unitId) {
        if (unitId == null) {
            return null;
        }

        Matcher matcher = UNIT_ID.matcher(unitId);

        if (!matcher.matches()) {
            return null;
        }

        return Integer.parseInt(matcher.group(1));
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

    private String toRoman(int number) {
        return switch (number) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            case 6 -> "VI";
            case 7 -> "VII";
            case 8 -> "VIII";
            case 9 -> "IX";
            case 10 -> "X";
            default -> String.valueOf(number);
        };
    }
}