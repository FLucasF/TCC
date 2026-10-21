package com.pousada.rateplan;

import com.pousada.model.RatePlanType;

import java.util.HashMap;
import java.util.Map;

public class RatePlanFactory {

    private static final Map<RatePlanType, RatePlan> PLANS = new HashMap<>();

    static {
        register(new StandardRatePlan());
        register(new NonRefundableRatePlan());
        register(new CorporateRatePlan());
        register(new WeekendPackageRatePlan());
        register(new LongStayRatePlan());
    }

    private static void register(RatePlan plan) {
        PLANS.put(plan.getType(), plan);
    }

    public static RatePlan create(RatePlanType type) {
        RatePlan plan = PLANS.get(type);

        try {
            return plan.getClass().getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao criar plano de tarifa", e);
        }
    }
}
