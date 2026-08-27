package com.greenink.api.pyq.dto;

public record PyqAnswerResponse(
        String questionId,
        String selectedOption,
        boolean correct,
        String correctOption,
        String explanation
) {}
