package com.greenink.api.pyq.dto;

public record PyqAttemptCompleteResponse(
        String attemptId,
        int answered,
        int correct,
        int total,
        int percentage,
        boolean progressSaved
) {}
