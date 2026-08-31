package com.greenink.api.pyq.dto;

import com.greenink.api.pyq.PyqQuestion;

import java.util.List;
import java.util.Map;

public record PyqQuestionResponse(
        String id,
        String question,
        List<PyqQuestion.Option> options,
        String source,
        List<List<String>> matchRows,
        String unkeyedStatus,
        List<Integer> kuralRefs,
        Map<String, Object> sourceMetadata
) {}
