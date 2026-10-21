package com.pousadamarealta.tariff;

import com.pousadamarealta.model.RatePlan;

public class TarifPlanFactory {
    public static TarifPlan create(RatePlan ratePlan) {
        return switch (ratePlan) {
            case STANDARD -> new StandardTarifPlan();
            case NON_REFUNDABLE -> new NonRefundableTarifPlan();
        };
    }
}
