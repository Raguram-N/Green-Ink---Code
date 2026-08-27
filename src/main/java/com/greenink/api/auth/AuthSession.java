package com.greenink.api.auth;

import java.time.Instant;

public record AuthSession(String refreshTokenHash, String userId, Instant expiresAt, Instant createdAt) {}
