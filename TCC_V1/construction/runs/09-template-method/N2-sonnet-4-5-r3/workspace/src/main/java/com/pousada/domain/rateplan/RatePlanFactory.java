package com.pousada.domain.rateplan;

import java.util.Map;

public class RatePlanFactory {
    private static final Map<String, RatePlan> PLANS = Map.of(
        "STANDARD", new StandardRatePlan(),
        "NON_REFUNDABLE", new NonRefundableRatePlan()
    );

    public static RatePlan get(String name) {
        RatePlan plan = PLANS.get(name);
        if (plan == null) {
            throw new IllegalArgumentException("Unknown rate plan: " + name);
        }
        return plan;
    }

    public static Map<String, RatePlan> getAll() {
        return PLANS;
    }
}
