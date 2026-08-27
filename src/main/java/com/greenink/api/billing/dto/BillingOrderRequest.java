package com.greenink.api.billing.dto;

import jakarta.validation.constraints.NotBlank;

public record BillingOrderRequest(@NotBlank String planCode) {}
