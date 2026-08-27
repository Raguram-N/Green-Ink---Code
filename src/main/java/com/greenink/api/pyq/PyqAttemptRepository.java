package com.greenink.api.pyq;

import java.util.Optional;

public interface PyqAttemptRepository {
    PyqAttempt save(PyqAttempt attempt);
    Optional<PyqAttempt> findById(String attemptId);
    void deleteAllByUserId(String userId);
}
