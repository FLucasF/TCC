package pousada.domain;

import java.util.List;
import java.util.Optional;

/** Os planos de tarifa existentes. Plano novo entra só nesta lista. */
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
        return ALL.stream().filter(p -> p.code().equals(code)).findFirst();
    }

    public static RatePlan get(String code) {
        return find(code).orElseThrow(() -> new IllegalStateException("Plano desconhecido: " + code));
    }
}
