package com.greenink.api.pyq;

import java.util.List;

public record PyqQuestion(
        String id,
        String chapterId,
        String question,
        List<Option> options,
        String correctOption,
        String explanation,
        String source
) {
    public record Option(String key, String text) {}
}
