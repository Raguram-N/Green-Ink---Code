package com.greenink.api.billing.dto;

public record BillingOrderResponse(
        String orderId,
        String planCode,
        long amount,
        String currency,
        String gatewayPublicKey
) {}
