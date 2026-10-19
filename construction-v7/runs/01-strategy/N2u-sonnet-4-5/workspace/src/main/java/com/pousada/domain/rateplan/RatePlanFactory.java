package com.pousada.domain.rateplan;

import com.pousada.domain.RatePlanType;

public class RatePlanFactory {

    public static RatePlan create(RatePlanType type) {
        return switch (type) {
            case STANDARD -> new StandardRatePlan();
            case NON_REFUNDABLE -> new NonRefundableRatePlan();
            case CORPORATE -> new CorporateRatePlan();
            case WEEKEND_PACKAGE -> new WeekendPackageRatePlan();
            case LONG_STAY -> new LongStayRatePlan();
        };
    }
}
