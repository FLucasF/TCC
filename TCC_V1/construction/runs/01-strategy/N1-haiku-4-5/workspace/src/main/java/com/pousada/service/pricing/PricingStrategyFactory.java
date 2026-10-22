package com.pousada.service.pricing;

import com.pousada.model.RatePlan;

public class PricingStrategyFactory {
    public static PricingStrategy create(RatePlan ratePlan, String companyCode) {
        return switch (ratePlan) {
            case STANDARD -> new StandardPricingStrategy();
            case NON_REFUNDABLE -> new NonRefundablePricingStrategy();
            case CORPORATE -> new CorporatePricingStrategy(companyCode);
            case WEEKEND_PACKAGE -> new WeekendPackagePricingStrategy();
            case LONG_STAY -> new LongStayPricingStrategy();
        };
    }
}
