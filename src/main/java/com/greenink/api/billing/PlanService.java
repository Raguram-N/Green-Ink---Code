package com.greenink.api.billing;

import com.greenink.api.billing.dto.PlanResponse;
import com.greenink.api.common.BadRequestException;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class PlanService {
    private final Map<String, PlanDefinition> plans = new LinkedHashMap<>();
    private static final List<String> METHODS = List.of("UPI", "GPAY", "CARD", "NETBANKING");

    public PlanService() {
        plans.put("MONTHLY", new PlanDefinition("MONTHLY", "1 Month", 19_900, "INR", 1));
        plans.put("SIX_MONTH", new PlanDefinition("SIX_MONTH", "6 Months", 79_900, "INR", 6));
        plans.put("YEARLY", new PlanDefinition("YEARLY", "1 Year", 99_900, "INR", 12));
    }

    public List<PlanResponse> publicPlans() {
        return plans.values().stream().map(p -> new PlanResponse(p.code(), p.name(), p.amountPaise(), p.currency(), METHODS)).toList();
    }

    public PlanDefinition require(String code) {
        if (code == null) throw new BadRequestException("PLAN_REQUIRED", "Plan code is required.");
        PlanDefinition plan = plans.get(code.toUpperCase());
        if (plan == null) throw new BadRequestException("PLAN_INVALID", "Unknown plan code.");
        return plan;
    }
}
