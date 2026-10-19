package com.pousada.service.pricing;

import com.pousada.domain.RatePlan;

public class PricingStrategyFactory {
    public static PricingStrategy getStrategy(RatePlan ratePlan) {
        return switch (ratePlan) {
            case STANDARD -> new StandardPricingStrategy();
            case NON_REFUNDABLE -> new NonRefundablePricingStrategy();
            case CORPORATE -> new CorporatePricingStrategy();
            case WEEKEND_PACKAGE -> new WeekendPackagePricingStrategy();
            case LONG_STAY -> new LongStayPricingStrategy();
        };
    }
}
