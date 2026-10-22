package com.pousadamaralta.domain.rateplan;

import com.pousadamaralta.domain.model.RatePlanType;
import java.util.Map;

public class RatePlanFactory {
    private static final Map<RatePlanType, RatePlan> PLANS = Map.of(
        RatePlanType.STANDARD, new StandardRatePlan(),
        RatePlanType.NON_REFUNDABLE, new NonRefundableRatePlan()
    );

    public static RatePlan get(RatePlanType type) {
        return PLANS.get(type);
    }
}
