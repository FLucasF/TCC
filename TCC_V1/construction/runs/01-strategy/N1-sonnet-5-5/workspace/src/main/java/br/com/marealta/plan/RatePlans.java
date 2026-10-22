package br.com.marealta.plan;

import java.util.List;
import java.util.Optional;

/** Único lugar que lista os planos de tarifa existentes. */
public final class RatePlans {

    private static final List<RatePlan> ALL = List.of(
            new StandardPlan(),
            new NonRefundablePlan(),
            new CorporatePlan(),
            new WeekendPackagePlan(),
            new LongStayPlan());

    private RatePlans() {
    }

    public static Optional<RatePlan> find(String code) {
        return ALL.stream().filter(plan -> plan.code().equals(code)).findFirst();
    }

    public static RatePlan get(String code) {
        return find(code).orElseThrow(() -> new IllegalStateException("Plano desconhecido: " + code));
    }
}
