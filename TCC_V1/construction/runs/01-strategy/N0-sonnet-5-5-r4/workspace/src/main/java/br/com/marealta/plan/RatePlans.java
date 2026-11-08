package br.com.marealta.plan;

import java.util.List;
import java.util.Optional;

/** Registro dos planos de tarifa disponíveis. */
public final class RatePlans {
    private static final List<RatePlan> ALL = List.of(
            new StandardPlan(),
            new NonRefundablePlan(),
            new CorporatePlan(),
            new WeekendPackagePlan(),
            new LongStayPlan());

    private RatePlans() {}

    public static Optional<RatePlan> find(String code) {
        return ALL.stream().filter(p -> p.code().equals(code)).findFirst();
    }
}
