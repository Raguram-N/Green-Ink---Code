package com.greenink.api.billing.dto;

import java.util.List;

public record PlanResponse(
        String code,
        String name,
        long amount,
        String currency,
        List<String> supportedMethods
) {}
