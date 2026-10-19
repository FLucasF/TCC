package com.pousadamareal.domain;

import com.pousadamareal.domain.rateplans.*;

import java.util.HashMap;
import java.util.Map;

public class RatePlanFactory {
    private static final Map<String, RatePlan> PLANS = new HashMap<>();

    static {
        PLANS.put("STANDARD", new StandardRatePlan());
        PLANS.put("NON_REFUNDABLE", new NonRefundableRatePlan());
        PLANS.put("CORPORATE", new CorporateRatePlan());
        PLANS.put("WEEKEND_PACKAGE", new WeekendPackageRatePlan());
        PLANS.put("LONG_STAY", new LongStayRatePlan());
    }

    public static RatePlan getPlan(String planName) {
        return PLANS.get(planName);
    }

    public static boolean isValidPlanName(String planName) {
        return PLANS.containsKey(planName);
    }
}
