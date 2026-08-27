package com.greenink.api.entitlement;

import java.time.Instant;

public record Subscription(
        String userId,
        String planCode,
        String status,
        Instant startedAt,
        Instant expiresAt,
        Instant updatedAt
) {
    public boolean activeAt(Instant now) {
        return "ACTIVE".equals(status) && expiresAt != null && now.isBefore(expiresAt);
    }
}
