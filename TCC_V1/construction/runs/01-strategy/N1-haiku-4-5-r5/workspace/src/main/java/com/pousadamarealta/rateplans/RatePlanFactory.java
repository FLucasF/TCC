package com.pousadamarealta.rateplans;

import java.util.HashMap;
import java.util.Map;

public class RatePlanFactory {
    private static final Map<String, RatePlan> PLANS = new HashMap<>();

    static {
        register(new StandardPlan());
        register(new NonRefundablePlan());
        register(new CorporatePlan());
        register(new WeekendPackagePlan());
        register(new LongStayPlan());
    }

    public static void register(RatePlan plan) {
        PLANS.put(plan.getName(), plan);
    }

    public static RatePlan getPlan(String name) {
        return PLANS.get(name);
    }

    public static boolean isPlanValid(String name) {
        return PLANS.containsKey(name);
    }
}
