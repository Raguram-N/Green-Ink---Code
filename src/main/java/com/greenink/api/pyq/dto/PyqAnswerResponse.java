package com.greenink.api.pyq.dto;

public record PyqAnswerResponse(
        String questionId,
        String selectedOption,
        boolean scored,
        Boolean correct,
        String correctOption,
        String explanation
) {}
