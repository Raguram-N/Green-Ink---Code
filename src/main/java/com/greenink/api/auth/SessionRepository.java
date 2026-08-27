package com.greenink.api.auth;

import java.util.Optional;

public interface SessionRepository {
    void save(AuthSession session);
    Optional<AuthSession> findByRefreshTokenHash(String refreshTokenHash);
    void deleteByRefreshTokenHash(String refreshTokenHash);
    void deleteAllByUserId(String userId);
}
