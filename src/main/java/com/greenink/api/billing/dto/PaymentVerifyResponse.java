package com.greenink.api.billing.dto;

import java.time.Instant;

public record PaymentVerifyResponse(
        String status,
        String planCode,
        boolean premium,
        Instant premiumUntil
) {}
