package com.pousada.rateplan;

import java.util.Map;

public class RatePlanFactory {
    private static final Map<String, RatePlan> PLANS = Map.of(
            "STANDARD", new StandardPlan(),
            "NON_REFUNDABLE", new NonRefundablePlan(),
            "CORPORATE", new CorporatePlan(),
            "WEEKEND_PACKAGE", new WeekendPackagePlan(),
            "LONG_STAY", new LongStayPlan()
    );

    public static RatePlan getPlan(String name) {
        return PLANS.get(name);
    }

    public static boolean exists(String name) {
        return PLANS.containsKey(name);
    }
}
