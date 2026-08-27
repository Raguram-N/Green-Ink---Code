package com.greenink.api.billing;

import java.time.Instant;

public record PaymentRecord(
        String orderId,
        String userId,
        String planCode,
        long amountPaise,
        String currency,
        String status,
        String providerPaymentId,
        Instant createdAt,
        Instant paidAt
) {}
