package com.greenink.api.infrastructure.memory;

import com.greenink.api.auth.AuthSession;
import com.greenink.api.auth.SessionRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class PlaceholderSessionRepository implements SessionRepository {
    private final ConcurrentHashMap<String, AuthSession> sessions = new ConcurrentHashMap<>();

    @Override public void save(AuthSession session) { sessions.put(session.refreshTokenHash(), session); }
    @Override public Optional<AuthSession> findByRefreshTokenHash(String hash) { return Optional.ofNullable(sessions.get(hash)); }
    @Override public void deleteByRefreshTokenHash(String hash) { sessions.remove(hash); }
    @Override public void deleteAllByUserId(String userId) { sessions.entrySet().removeIf(e -> e.getValue().userId().equals(userId)); }
}
