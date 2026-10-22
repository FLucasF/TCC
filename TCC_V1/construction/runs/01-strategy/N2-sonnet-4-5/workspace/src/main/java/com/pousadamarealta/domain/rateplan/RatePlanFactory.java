package com.pousadamarealta.domain.rateplan;

import com.pousadamarealta.domain.RatePlanType;
import java.util.Map;
import java.util.function.Supplier;

public class RatePlanFactory {

    private static final Map<RatePlanType, Supplier<RatePlan>> RATE_PLANS = Map.of(
        RatePlanType.STANDARD, StandardRatePlan::new,
        RatePlanType.NON_REFUNDABLE, NonRefundableRatePlan::new,
        RatePlanType.CORPORATE, CorporateRatePlan::new,
        RatePlanType.WEEKEND_PACKAGE, WeekendPackageRatePlan::new,
        RatePlanType.LONG_STAY, LongStayRatePlan::new
    );

    public static RatePlan create(RatePlanType type) {
        Supplier<RatePlan> supplier = RATE_PLANS.get(type);
        if (supplier == null) {
            throw new IllegalArgumentException("Unknown rate plan type: " + type);
        }
        return supplier.get();
    }
}
