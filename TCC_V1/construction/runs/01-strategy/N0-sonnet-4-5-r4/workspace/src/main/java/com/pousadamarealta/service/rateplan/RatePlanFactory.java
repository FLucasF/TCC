package com.pousadamarealta.service.rateplan;

import com.pousadamarealta.model.RatePlan;
import com.pousadamarealta.service.PricingService;

public class RatePlanFactory {
    private final PricingService pricingService;

    public RatePlanFactory(PricingService pricingService) {
        this.pricingService = pricingService;
    }

    public RatePlanStrategy getStrategy(RatePlan ratePlan) {
        return switch (ratePlan) {
            case STANDARD -> new StandardRatePlan(pricingService);
            case NON_REFUNDABLE -> new NonRefundableRatePlan(pricingService);
            case CORPORATE -> new CorporateRatePlan(pricingService);
            case WEEKEND_PACKAGE -> new WeekendPackageRatePlan(pricingService);
            case LONG_STAY -> new LongStayRatePlan(pricingService);
        };
    }
}
