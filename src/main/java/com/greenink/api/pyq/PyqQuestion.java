package com.greenink.api.pyq;

import java.util.List;
import java.util.Map;

public record PyqQuestion(
        String id,
        String chapterId,
        String question,
        List<Option> options,
        String correctOption,
        String explanation,
        String source,
        List<List<String>> matchRows,
        String unkeyedStatus,
        List<Integer> kuralRefs,
        Map<String, Object> sourceMetadata
) {
    public PyqQuestion(
            String id,
            String chapterId,
            String question,
            List<Option> options,
            String correctOption,
            String explanation,
            String source
    ) {
        this(id, chapterId, question, options, correctOption, explanation, source,
                List.of(), null, List.of(), Map.of());
    }

    public record Option(String key, String text) {}
}
