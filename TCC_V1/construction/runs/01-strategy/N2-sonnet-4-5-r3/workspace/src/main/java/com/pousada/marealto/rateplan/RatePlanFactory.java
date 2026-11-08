package com.pousada.marealto.rateplan;

import com.pousada.marealto.domain.RatePlan;

import java.util.Map;

public class RatePlanFactory {
    private static final Map<RatePlan, RatePlanStrategy> STRATEGIES = Map.of(
        RatePlan.STANDARD, new StandardRatePlan(),
        RatePlan.NON_REFUNDABLE, new NonRefundableRatePlan(),
        RatePlan.CORPORATE, new CorporateRatePlan(),
        RatePlan.WEEKEND_PACKAGE, new WeekendPackageRatePlan(),
        RatePlan.LONG_STAY, new LongStayRatePlan()
    );

    public static RatePlanStrategy getStrategy(RatePlan plan) {
        return STRATEGIES.get(plan);
    }
}
