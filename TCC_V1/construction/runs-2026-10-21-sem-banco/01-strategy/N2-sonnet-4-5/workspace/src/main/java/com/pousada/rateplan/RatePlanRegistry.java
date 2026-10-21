package com.pousada.rateplan;

import com.pousada.domain.RatePlan;
import java.util.EnumMap;
import java.util.Map;

public class RatePlanRegistry {

    private static final Map<RatePlan, RatePlanStrategy> strategies = new EnumMap<>(RatePlan.class);

    static {
        strategies.put(RatePlan.STANDARD, new StandardRatePlan());
        strategies.put(RatePlan.NON_REFUNDABLE, new NonRefundableRatePlan());
        strategies.put(RatePlan.CORPORATE, new CorporateRatePlan());
        strategies.put(RatePlan.WEEKEND_PACKAGE, new WeekendPackageRatePlan());
        strategies.put(RatePlan.LONG_STAY, new LongStayRatePlan());
    }

    public static RatePlanStrategy getStrategy(RatePlan ratePlan) {
        return strategies.get(ratePlan);
    }
}
