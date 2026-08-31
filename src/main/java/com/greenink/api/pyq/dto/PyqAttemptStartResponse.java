package com.greenink.api.pyq.dto;

import java.util.List;
import java.util.Map;

public record PyqAttemptStartResponse(
        String attemptId,
        String chapterId,
        int totalQuestions,
        List<PyqQuestionResponse> questions,
        String guestAttemptToken,
        Map<String, List<String>> kuralText
) {}
