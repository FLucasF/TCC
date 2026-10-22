package com.pousada.plan;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Único lugar que conhece os planos existentes: um plano novo entra só nesta lista. */
public class RatePlans {

    private final Map<String, RatePlan> byCode;

    public RatePlans(List<RatePlan> plans) {
        this.byCode = plans.stream().collect(Collectors.toUnmodifiableMap(RatePlan::code, Function.identity()));
    }

    public static RatePlans defaults() {
        return new RatePlans(List.of(
                new StandardPlan(),
                new NonRefundablePlan(),
                new CorporatePlan(),
                new WeekendPackagePlan(),
                new LongStayPlan()));
    }

    public Optional<RatePlan> find(String code) {
        return Optional.ofNullable(byCode.get(code));
    }

    public RatePlan get(String code) {
        return find(code).orElseThrow(() -> new IllegalStateException("Plano desconhecido: " + code));
    }
}
