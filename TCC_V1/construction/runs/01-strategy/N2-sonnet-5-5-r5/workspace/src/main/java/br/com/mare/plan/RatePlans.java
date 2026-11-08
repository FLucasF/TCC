package br.com.mare.plan;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Único lugar que conhece todos os planos de tarifa. Um plano novo entra aqui. */
public final class RatePlans {
    private static final Map<String, RatePlan> BY_CODE = List.<RatePlan>of(
                    new StandardPlan(),
                    new NonRefundablePlan(),
                    new CorporatePlan(),
                    new WeekendPackagePlan(),
                    new LongStayPlan())
            .stream().collect(Collectors.toUnmodifiableMap(RatePlan::code, Function.identity()));

    private RatePlans() {}

    public static Optional<RatePlan> find(String code) {
        return Optional.ofNullable(BY_CODE.get(code));
    }
}
