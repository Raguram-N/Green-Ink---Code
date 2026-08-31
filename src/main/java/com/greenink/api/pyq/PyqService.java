package com.greenink.api.pyq;

import com.greenink.api.catalog.CatalogService;
import com.greenink.api.catalog.ChapterDefinition;
import com.greenink.api.common.BadRequestException;
import com.greenink.api.common.Hashing;
import com.greenink.api.common.NotFoundException;
import com.greenink.api.common.UnauthorizedException;
import com.greenink.api.entitlement.EntitlementService;
import com.greenink.api.progress.ProgressService;
import com.greenink.api.pyq.dto.PyqAnswerRequest;
import com.greenink.api.pyq.dto.PyqAnswerResponse;
import com.greenink.api.pyq.dto.PyqAttemptCompleteResponse;
import com.greenink.api.pyq.dto.PyqAttemptStartResponse;
import com.greenink.api.pyq.dto.PyqQuestionResponse;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class PyqService {
    private final CatalogService catalogService;
    private final EntitlementService entitlementService;
    private final PyqRepository pyqRepository;
    private final PyqAttemptRepository attemptRepository;
    private final ProgressService progressService;
    private final SecureRandom secureRandom = new SecureRandom();

    public PyqService(CatalogService catalogService, EntitlementService entitlementService, PyqRepository pyqRepository,
                      PyqAttemptRepository attemptRepository, ProgressService progressService) {
        this.catalogService = catalogService;
        this.entitlementService = entitlementService;
        this.pyqRepository = pyqRepository;
        this.attemptRepository = attemptRepository;
        this.progressService = progressService;
    }

    public PyqAttemptStartResponse start(String chapterId, Optional<String> userId) {
        ChapterDefinition chapter = catalogService.requireChapter(chapterId);
        entitlementService.requireChapterAccess(chapter, userId);
        List<PyqQuestion> questions = pyqRepository.findByChapterId(chapterId);
        if (questions.isEmpty()) throw new NotFoundException("PYQ_NOT_MIGRATED", "PYQ data is not present in the placeholder repository for this chapter yet.");

        String guestToken = userId.isEmpty() ? newGuestToken() : null;
        String attemptId = "att_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        PyqAttempt attempt = new PyqAttempt(
                attemptId, chapterId, userId.orElse(null), guestToken == null ? null : Hashing.sha256(guestToken),
                questions.stream().map(PyqQuestion::id).toList(), Instant.now());
        attemptRepository.save(attempt);
        List<PyqQuestionResponse> publicQuestions = questions.stream()
                .map(q -> new PyqQuestionResponse(q.id(), q.question(), q.options(), q.source(), q.matchRows(), q.unkeyedStatus(), q.kuralRefs(), q.sourceMetadata()))
                .toList();
        return new PyqAttemptStartResponse(attemptId, chapterId, questions.size(), publicQuestions, guestToken, pyqRepository.kuralTextByChapterId(chapterId));
    }

    public PyqAnswerResponse answer(String attemptId, String guestToken, Optional<String> userId, PyqAnswerRequest request) {
        PyqAttempt attempt = requireAttempt(attemptId);
        assertOwner(attempt, userId, guestToken);
        if (!attempt.questionIds().contains(request.questionId())) {
            throw new BadRequestException("QUESTION_NOT_IN_ATTEMPT", "Question does not belong to this attempt.");
        }
        PyqQuestion question = pyqRepository.findById(request.questionId())
                .orElseThrow(() -> new NotFoundException("QUESTION_NOT_FOUND", "Question not found."));
        String selected = request.selectedOption().toUpperCase();
        boolean validOption = question.options().stream().anyMatch(o -> o.key().equalsIgnoreCase(selected));
        if (!validOption) throw new BadRequestException("INVALID_OPTION", "Selected option is invalid.");
        try {
            attempt.answer(question.id(), selected);
        } catch (IllegalStateException ex) {
            throw new BadRequestException("ATTEMPT_ALREADY_COMPLETED", ex.getMessage());
        }
        attemptRepository.save(attempt);
        boolean scored = isScoreable(question);
        Boolean correct = scored ? question.correctOption().equalsIgnoreCase(selected) : null;
        String explanation = scored ? question.explanation() : "Answer key unavailable";
        return new PyqAnswerResponse(question.id(), selected, scored, correct,
                scored ? question.correctOption() : null, explanation);
    }

    public PyqAttemptCompleteResponse complete(String attemptId, String guestToken, Optional<String> userId) {
        PyqAttempt attempt = requireAttempt(attemptId);
        assertOwner(attempt, userId, guestToken);
        Map<String, String> answers = attempt.answers();
        int correct = 0;
        for (Map.Entry<String, String> entry : answers.entrySet()) {
            PyqQuestion question = pyqRepository.findById(entry.getKey()).orElse(null);
            if (question != null && isScoreable(question) && question.correctOption().equalsIgnoreCase(entry.getValue())) correct++;
        }
        attempt.complete();
        attemptRepository.save(attempt);
        boolean saved = false;
        if (attempt.userId() != null) {
            progressService.recordPyqAttempt(attempt.userId(), attempt.chapterId(), answers.size(), correct);
            saved = true;
        }
        int total = attempt.questionIds().size();
        int scoreableTotal = (int) attempt.questionIds().stream()
                .map(id -> pyqRepository.findById(id).orElse(null))
                .filter(this::isScoreable)
                .count();
        int percentage = scoreableTotal == 0 ? 0 : (int) Math.round(correct * 100.0 / scoreableTotal);
        return new PyqAttemptCompleteResponse(attempt.id(), answers.size(), correct, total, scoreableTotal, percentage, saved);
    }

    private PyqAttempt requireAttempt(String attemptId) {
        return attemptRepository.findById(attemptId)
                .orElseThrow(() -> new NotFoundException("ATTEMPT_NOT_FOUND", "PYQ attempt not found."));
    }

    private void assertOwner(PyqAttempt attempt, Optional<String> userId, String guestToken) {
        if (attempt.userId() != null) {
            if (userId.isPresent() && attempt.userId().equals(userId.get())) return;
            throw new UnauthorizedException("ATTEMPT_OWNER_MISMATCH", "This attempt belongs to another account.");
        }
        if (guestToken != null && attempt.guestTokenHash() != null && Hashing.sha256(guestToken).equals(attempt.guestTokenHash())) return;
        throw new UnauthorizedException("GUEST_ATTEMPT_TOKEN_REQUIRED", "X-Attempt-Token is required for this guest attempt.");
    }

    private boolean isScoreable(PyqQuestion question) {
        return question != null && question.correctOption() != null && !question.correctOption().isBlank();
    }

    private String newGuestToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
