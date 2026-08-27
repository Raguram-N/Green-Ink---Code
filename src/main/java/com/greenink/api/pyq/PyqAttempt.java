package com.greenink.api.pyq;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PyqAttempt {
    private final String id;
    private final String chapterId;
    private final String userId;
    private final String guestTokenHash;
    private final List<String> questionIds;
    private final Instant createdAt;
    private final Map<String, String> answers = new LinkedHashMap<>();
    private Instant completedAt;

    public PyqAttempt(String id, String chapterId, String userId, String guestTokenHash, List<String> questionIds, Instant createdAt) {
        this.id = id;
        this.chapterId = chapterId;
        this.userId = userId;
        this.guestTokenHash = guestTokenHash;
        this.questionIds = List.copyOf(questionIds);
        this.createdAt = createdAt;
    }

    public String id() { return id; }
    public String chapterId() { return chapterId; }
    public String userId() { return userId; }
    public String guestTokenHash() { return guestTokenHash; }
    public List<String> questionIds() { return questionIds; }
    public Instant createdAt() { return createdAt; }
    public synchronized Map<String, String> answers() { return Map.copyOf(answers); }
    public synchronized Instant completedAt() { return completedAt; }
    public synchronized boolean completed() { return completedAt != null; }

    public synchronized void answer(String questionId, String selectedOption) {
        if (completed()) throw new IllegalStateException("Attempt is already completed");
        answers.put(questionId, selectedOption);
    }

    public synchronized void complete() {
        if (completedAt == null) completedAt = Instant.now();
    }
}
