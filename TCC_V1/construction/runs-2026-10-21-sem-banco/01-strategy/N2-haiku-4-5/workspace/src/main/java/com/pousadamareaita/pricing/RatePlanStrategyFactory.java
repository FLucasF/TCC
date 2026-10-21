package com.pousadamareaita.pricing;

import com.pousadamareaita.model.RatePlan;
import com.pousadamareaita.model.RoomType;

public class RatePlanStrategyFactory {
    public static RatePlanStrategy create(RatePlan ratePlan, RoomType roomType, String companyCode) {
        return switch (ratePlan) {
            case STANDARD -> new StandardPlanStrategy();
            case NON_REFUNDABLE -> new NonRefundablePlanStrategy();
            case CORPORATE -> new CorporatePlanStrategy(companyCode);
            case WEEKEND_PACKAGE -> new WeekendPackagePlanStrategy(roomType);
            case LONG_STAY -> new LongStayPlanStrategy();
        };
    }
}
