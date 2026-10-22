package br.com.marealta.plan;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Cadastro dos planos de tarifa disponíveis. Plano novo = nova classe + uma linha aqui. */
public final class RatePlans {
    private static final Map<String, RatePlan> BY_CODE = new LinkedHashMap<>();

    static {
        for (RatePlan p : List.of(
                new StandardPlan(),
                new NonRefundablePlan(),
                new CorporatePlan(),
                new WeekendPackagePlan(),
                new LongStayPlan())) {
            BY_CODE.put(p.code(), p);
        }
    }

    private RatePlans() {}

    public static Optional<RatePlan> find(String code) {
        return Optional.ofNullable(BY_CODE.get(code));
    }
}
