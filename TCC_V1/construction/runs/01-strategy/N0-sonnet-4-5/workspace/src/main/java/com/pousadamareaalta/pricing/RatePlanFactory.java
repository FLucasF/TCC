package com.pousadamareaalta.pricing;

import com.pousadamareaalta.model.RatePlan;

public class RatePlanFactory {

    public static RatePlanStrategy getStrategy(RatePlan ratePlan) {
        return switch (ratePlan) {
            case STANDARD -> new StandardRatePlan();
            case NON_REFUNDABLE -> new NonRefundableRatePlan();
            case CORPORATE -> new CorporateRatePlan();
            case WEEKEND_PACKAGE -> new WeekendPackageRatePlan();
            case LONG_STAY -> new LongStayRatePlan();
        };
    }
}
