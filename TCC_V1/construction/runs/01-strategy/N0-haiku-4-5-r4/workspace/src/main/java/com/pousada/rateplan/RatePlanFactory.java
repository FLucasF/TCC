package com.pousada.rateplan;

import java.util.HashMap;
import java.util.List;
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

    private static void register(RatePlan plan) {
        PLANS.put(plan.getName(), plan);
    }

    public static RatePlan getPlan(String planName) {
        return PLANS.get(planName);
    }

    public static boolean isValidPlan(String planName) {
        return PLANS.containsKey(planName);
    }

    public static List<String> getAllPlans() {
        return List.copyOf(PLANS.keySet());
    }
}
