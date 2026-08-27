package com.greenink.api.billing.dto;

import jakarta.validation.constraints.NotBlank;

public record PaymentVerifyRequest(
        @NotBlank String orderId,
        @NotBlank String paymentId,
        @NotBlank String signature
) {}
