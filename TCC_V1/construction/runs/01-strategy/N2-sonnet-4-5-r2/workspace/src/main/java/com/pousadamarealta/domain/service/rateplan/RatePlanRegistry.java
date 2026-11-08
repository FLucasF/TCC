package com.pousadamarealta.domain.service.rateplan;

import com.pousadamarealta.domain.model.RatePlanType;

import java.util.Map;

public class RatePlanRegistry {
    private static final Map<RatePlanType, RatePlan> PLANS = Map.of(
        RatePlanType.STANDARD, new StandardRatePlan(),
        RatePlanType.NON_REFUNDABLE, new NonRefundableRatePlan(),
        RatePlanType.CORPORATE, new CorporateRatePlan(),
        RatePlanType.WEEKEND_PACKAGE, new WeekendPackageRatePlan(),
        RatePlanType.LONG_STAY, new LongStayRatePlan()
    );

    public static RatePlan get(RatePlanType type) {
        return PLANS.get(type);
    }
}
