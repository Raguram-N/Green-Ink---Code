package com.greenink.api.pyq.dto;

import java.util.List;

public record PyqAttemptStartResponse(
        String attemptId,
        String chapterId,
        int totalQuestions,
        List<PyqQuestionResponse> questions,
        String guestAttemptToken
) {}
