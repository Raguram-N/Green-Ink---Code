package com.greenink.api.infrastructure.jooq;

import com.greenink.api.pyq.PyqQuestion;
import com.greenink.api.pyq.PyqRepository;
import com.greenink.jooq.generated.tables.records.QuestionAppearancesRecord;
import com.greenink.jooq.generated.tables.records.QuestionsRecord;
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

import static com.greenink.jooq.generated.tables.QuestionAppearances.QUESTION_APPEARANCES;
import static com.greenink.jooq.generated.tables.Questions.QUESTIONS;
import static com.greenink.jooq.generated.tables.SyllabusUnits.SYLLABUS_UNITS;
import static com.greenink.jooq.generated.tables.Topics.TOPICS;

@Repository
@Profile("jooq")
public class JooqPyqRepository implements PyqRepository {

    private static final Pattern CHAPTER_ID =
            Pattern.compile("^u(\\d+)-c(\\d+)$");

    private final DSLContext dsl;

    public JooqPyqRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    @Override
    public List<PyqQuestion> findByChapterId(String chapterId) {
        Long syllabusUnitId = findSyllabusUnitId(chapterId);

        if (syllabusUnitId == null) {
            return List.of();
        }

        List<QuestionsRecord> questions = dsl
                .selectFrom(QUESTIONS)
                .where(QUESTIONS.UNIT_ID.eq(syllabusUnitId))
                .and(QUESTIONS.IS_ACTIVE.eq(true))
                .orderBy(QUESTIONS.ID)
                .fetch();

        if (questions.isEmpty()) {
            return List.of();
        }

        List<Long> questionIds = questions.stream()
                .map(q -> q.get(QUESTIONS.ID))
                .toList();

        Map<Long, List<Map<String, Object>>> appearances =
                loadAppearances(questionIds);

        return questions.stream()
                .map(question -> toDomain(
                        question,
                        chapterId,
                        appearances.getOrDefault(
                                question.get(QUESTIONS.ID),
                                List.of()
                        )
                ))
                .toList();
    }

    @Override
    public Optional<PyqQuestion> findById(String questionId) {
        Long databaseId = parseQuestionId(questionId);

        if (databaseId == null) {
            return Optional.empty();
        }

        QuestionsRecord question = dsl
                .selectFrom(QUESTIONS)
                .where(QUESTIONS.ID.eq(databaseId))
                .and(QUESTIONS.IS_ACTIVE.eq(true))
                .fetchOne();

        if (question == null) {
            return Optional.empty();
        }

        String chapterId = findChapterId(
                question.get(QUESTIONS.UNIT_ID)
        );

        if (chapterId == null) {
            return Optional.empty();
        }

        Map<Long, List<Map<String, Object>>> appearances =
                loadAppearances(List.of(databaseId));

        return Optional.of(
                toDomain(
                        question,
                        chapterId,
                        appearances.getOrDefault(
                                databaseId,
                                List.of()
                        )
                )
        );
    }

    @Override
    public int totalQuestionCount() {
        Integer count = dsl
                .selectCount()
                .from(QUESTIONS)
                .where(QUESTIONS.IS_ACTIVE.eq(true))
                .fetchOne(0, Integer.class);

        return count == null ? 0 : count;
    }

    private PyqQuestion toDomain(
            QuestionsRecord question,
            String chapterId,
            List<Map<String, Object>> appearances
    ) {
        List<PyqQuestion.Option> options = new ArrayList<>();

        addOption(
                options,
                "A",
                question.get(QUESTIONS.OPT_1_TEXT_PRIMARY)
        );

        addOption(
                options,
                "B",
                question.get(QUESTIONS.OPT_2_TEXT_PRIMARY)
        );

        addOption(
                options,
                "C",
                question.get(QUESTIONS.OPT_3_TEXT_PRIMARY)
        );

        addOption(
                options,
                "D",
                question.get(QUESTIONS.OPT_4_TEXT_PRIMARY)
        );

        Map<String, Object> metadata = new LinkedHashMap<>();

        String questionType =
                question.get(QUESTIONS.QUESTION_TYPE);

        if (questionType != null) {
            metadata.put("questionType", questionType);
        }

        if (!appearances.isEmpty()) {
            metadata.put("appearances", appearances);
        }

        return new PyqQuestion(
                apiQuestionId(question.get(QUESTIONS.ID)),
                chapterId,
                question.get(QUESTIONS.QUESTION_TEXT_PRIMARY),
                List.copyOf(options),
                toPublicOptionKey(
                        question.get(QUESTIONS.CORRECT_OPTION_KEY)
                ),
                question.get(QUESTIONS.EXPLANATION_PRIMARY),
                question.get(QUESTIONS.SOURCE_REFERENCE),
                List.of(),
                null,
                List.of(),
                Map.copyOf(metadata)
        );
    }

    private void addOption(
            List<PyqQuestion.Option> options,
            String key,
            String text
    ) {
        if (text != null && !text.isBlank()) {
            options.add(new PyqQuestion.Option(key, text));
        }
    }

    private Map<Long, List<Map<String, Object>>> loadAppearances(
            List<Long> questionIds
    ) {
        if (questionIds.isEmpty()) {
            return Map.of();
        }

        List<QuestionAppearancesRecord> records = dsl
                .selectFrom(QUESTION_APPEARANCES)
                .where(
                        QUESTION_APPEARANCES.QUESTION_ID.in(
                                questionIds
                        )
                )
                .orderBy(
                        QUESTION_APPEARANCES.EXAM_YEAR.desc(),
                        QUESTION_APPEARANCES.EXAM_CATEGORY,
                        QUESTION_APPEARANCES.EXAM_STAGE,
                        QUESTION_APPEARANCES.QUESTION_PAPER_QNO
                )
                .fetch();

        Map<Long, List<Map<String, Object>>> result =
                new LinkedHashMap<>();

        for (QuestionAppearancesRecord record : records) {
            Map<String, Object> appearance =
                    new LinkedHashMap<>();

            appearance.put(
                    "examCategory",
                    record.get(
                            QUESTION_APPEARANCES.EXAM_CATEGORY
                    )
            );

            appearance.put(
                    "examStage",
                    record.get(
                            QUESTION_APPEARANCES.EXAM_STAGE
                    )
            );

            appearance.put(
                    "examYear",
                    record.get(
                            QUESTION_APPEARANCES.EXAM_YEAR
                    )
            );

            appearance.put(
                    "questionNumber",
                    record.get(
                            QUESTION_APPEARANCES.QUESTION_PAPER_QNO
                    )
            );

            result.computeIfAbsent(
                    record.get(
                            QUESTION_APPEARANCES.QUESTION_ID
                    ),
                    ignored -> new ArrayList<>()
            ).add(Map.copyOf(appearance));
        }

        return result;
    }

    private Long findSyllabusUnitId(String chapterId) {
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
                .and(SYLLABUS_UNITS.PYQ_AVAILABLE.eq(true))
                .fetchOne(SYLLABUS_UNITS.ID);
    }

    private String findChapterId(Long syllabusUnitId) {
        if (syllabusUnitId == null) {
            return null;
        }

        var record = dsl
                .select(
                        TOPICS.DISPLAY_ORDER,
                        SYLLABUS_UNITS.DISPLAY_ORDER
                )
                .from(SYLLABUS_UNITS)
                .join(TOPICS)
                .on(
                        TOPICS.ID.eq(
                                SYLLABUS_UNITS.TOPIC_ID
                        )
                )
                .where(
                        SYLLABUS_UNITS.ID.eq(
                                syllabusUnitId
                        )
                )
                .fetchOne();

        if (record == null) {
            return null;
        }

        return "u"
                + record.get(TOPICS.DISPLAY_ORDER)
                + "-c"
                + record.get(
                        SYLLABUS_UNITS.DISPLAY_ORDER
                );
    }

    private String toPublicOptionKey(String databaseKey) {
        if (databaseKey == null) {
            return null;
        }

        return switch (databaseKey) {
            case "opt_1" -> "A";
            case "opt_2" -> "B";
            case "opt_3" -> "C";
            case "opt_4" -> "D";
            default -> null;
        };
    }

    private String apiQuestionId(Long databaseId) {
        return databaseId == null
                ? null
                : "q_" + databaseId;
    }

    private Long parseQuestionId(String questionId) {
        if (questionId == null || questionId.isBlank()) {
            return null;
        }

        String raw = questionId.startsWith("q_")
                ? questionId.substring(2)
                : questionId;

        try {
            return Long.valueOf(raw);
        } catch (NumberFormatException ex) {
            return null;
        }
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