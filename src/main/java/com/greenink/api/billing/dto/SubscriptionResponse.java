package com.greenink.api.billing.dto;

import java.time.Instant;

public record SubscriptionResponse(
        String status,
        String planCode,
        boolean premium,
        Instant startedAt,
        Instant expiresAt
) {}
