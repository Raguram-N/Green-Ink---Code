package com.greenink.api.infrastructure.memory;

import com.greenink.api.pyq.PyqAttempt;
import com.greenink.api.pyq.PyqAttemptRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class PlaceholderPyqAttemptRepository implements PyqAttemptRepository {
    private final ConcurrentHashMap<String, PyqAttempt> attempts = new ConcurrentHashMap<>();
    @Override public PyqAttempt save(PyqAttempt attempt) { attempts.put(attempt.id(), attempt); return attempt; }
    @Override public Optional<PyqAttempt> findById(String attemptId) { return Optional.ofNullable(attempts.get(attemptId)); }
    @Override public void deleteAllByUserId(String userId) { attempts.entrySet().removeIf(e -> userId.equals(e.getValue().userId())); }
}
