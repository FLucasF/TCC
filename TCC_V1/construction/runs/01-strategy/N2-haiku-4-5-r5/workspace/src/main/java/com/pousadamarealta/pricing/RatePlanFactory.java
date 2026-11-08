package com.pousadamarealta.pricing;

import com.pousadamarealta.domain.RatePlanType;

public class RatePlanFactory {
    public static RatePlan createRatePlan(RatePlanType type) {
        return switch (type) {
            case STANDARD -> new StandardPlan();
            case NON_REFUNDABLE -> new NonRefundablePlan();
            case CORPORATE -> new CorporatePlan();
            case WEEKEND_PACKAGE -> new WeekendPackagePlan();
            case LONG_STAY -> new LongStayPlan();
        };
    }
}
