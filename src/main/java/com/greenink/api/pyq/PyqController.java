package com.greenink.api.pyq;

import com.greenink.api.pyq.dto.PyqAnswerRequest;
import com.greenink.api.pyq.dto.PyqAnswerResponse;
import com.greenink.api.pyq.dto.PyqAttemptCompleteResponse;
import com.greenink.api.pyq.dto.PyqAttemptStartResponse;
import com.greenink.api.security.SecurityUtil;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class PyqController {
    private final PyqService pyqService;

    public PyqController(PyqService pyqService) { this.pyqService = pyqService; }

    @PostMapping("/chapters/{chapterId}/pyq/attempts")
    public PyqAttemptStartResponse start(@PathVariable String chapterId) {
        return pyqService.start(chapterId, SecurityUtil.currentUserId());
    }

    @PostMapping("/pyq/attempts/{attemptId}/answers")
    public PyqAnswerResponse answer(
            @PathVariable String attemptId,
            @RequestHeader(name = "X-Attempt-Token", required = false) String guestAttemptToken,
            @Valid @RequestBody PyqAnswerRequest request) {
        return pyqService.answer(attemptId, guestAttemptToken, SecurityUtil.currentUserId(), request);
    }

    @PostMapping("/pyq/attempts/{attemptId}/complete")
    public PyqAttemptCompleteResponse complete(
            @PathVariable String attemptId,
            @RequestHeader(name = "X-Attempt-Token", required = false) String guestAttemptToken) {
        return pyqService.complete(attemptId, guestAttemptToken, SecurityUtil.currentUserId());
    }
}
