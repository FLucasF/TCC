package com.pousada.domain.rateplans;

import java.util.HashMap;
import java.util.Map;

public class RatePlanRegistry {
    private static final Map<String, RatePlan> plans = new HashMap<>();

    static {
        plans.put("STANDARD", new StandardRatePlan());
        plans.put("NON_REFUNDABLE", new NonRefundableRatePlan());
        plans.put("CORPORATE", new CorporateRatePlan());
        plans.put("WEEKEND_PACKAGE", new WeekendPackageRatePlan());
        plans.put("LONG_STAY", new LongStayRatePlan());
    }

    public static RatePlan get(String id) {
        RatePlan plan = plans.get(id);
        if (plan == null) {
            throw new IllegalArgumentException("Plano de tarifa desconhecido: " + id);
        }
        return plan;
    }

    public static boolean exists(String id) {
        return plans.containsKey(id);
    }
}
