package br.com.marealta.plan;

import br.com.marealta.ApiException;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Registro dos planos de tarifa disponíveis. */
public final class RatePlans {
    private static final Map<String, RatePlan> BY_CODE = List.of(
                    new PercentagePlan("STANDARD", 0, true),
                    new PercentagePlan("NON_REFUNDABLE", 15, false),
                    new CorporatePlan(),
                    new WeekendPackagePlan(),
                    new LongStayPlan())
            .stream().collect(Collectors.toUnmodifiableMap(RatePlan::code, Function.identity()));

    private RatePlans() {}

    public static RatePlan get(String code) {
        RatePlan p = code == null ? null : BY_CODE.get(code);
        if (p == null) {
            throw ApiException.invalid("ratePlan inválido: " + code);
        }
        return p;
    }
}
