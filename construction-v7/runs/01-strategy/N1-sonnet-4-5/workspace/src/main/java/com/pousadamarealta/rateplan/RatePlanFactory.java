package com.pousadamarealta.rateplan;

import com.pousadamarealta.model.RatePlanType;

import java.util.EnumMap;
import java.util.Map;

public class RatePlanFactory {
    private static final Map<RatePlanType, RatePlan> RATE_PLANS = new EnumMap<>(RatePlanType.class);

    static {
        RATE_PLANS.put(RatePlanType.STANDARD, new StandardRatePlan());
        RATE_PLANS.put(RatePlanType.NON_REFUNDABLE, new NonRefundableRatePlan());
        RATE_PLANS.put(RatePlanType.CORPORATE, new CorporateRatePlan());
        RATE_PLANS.put(RatePlanType.WEEKEND_PACKAGE, new WeekendPackageRatePlan());
        RATE_PLANS.put(RatePlanType.LONG_STAY, new LongStayRatePlan());
    }

    public static RatePlan getRatePlan(RatePlanType type) {
        return RATE_PLANS.get(type);
    }
}
