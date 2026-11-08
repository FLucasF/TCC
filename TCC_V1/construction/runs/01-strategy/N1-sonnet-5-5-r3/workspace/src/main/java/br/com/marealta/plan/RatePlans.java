package br.com.marealta.plan;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Catálogo dos planos de tarifa. Para acrescentar um plano, é só listá-lo aqui. */
public final class RatePlans {
    private static final Map<String, RatePlan> BY_CODE = List.<RatePlan>of(
            new StandardPlan(),
            new NonRefundablePlan(),
            new CorporatePlan(),
            new WeekendPackagePlan(),
            new LongStayPlan())
            .stream().collect(Collectors.toMap(RatePlan::code, Function.identity()));

    private RatePlans() {
    }

    /** @return o plano, ou null se o código não existir. */
    public static RatePlan find(String code) {
        return BY_CODE.get(code);
    }
}
