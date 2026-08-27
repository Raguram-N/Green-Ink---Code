package com.greenink.api.billing;

public record PlanDefinition(String code, String name, long amountPaise, String currency, int months) {}
