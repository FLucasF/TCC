package br.com.marealta.plan;

import java.util.List;
import java.util.Optional;

/** O único lugar que conhece todos os planos de tarifa. */
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
}
