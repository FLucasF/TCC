package br.com.marealta.plan;

import br.com.marealta.domain.BusinessException;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** O único lugar que conhece os planos de tarifa: um plano novo entra na lista abaixo. */
public final class RatePlans {
    private static final Map<String, RatePlan> BY_CODE = List.<RatePlan>of(
                    new StandardPlan(),
                    new NonRefundablePlan(),
                    new CorporatePlan(),
                    new WeekendPackagePlan(),
                    new LongStayPlan())
            .stream().collect(Collectors.toUnmodifiableMap(RatePlan::code, Function.identity()));

    private RatePlans() {
    }

    public static RatePlan byCode(String code) {
        RatePlan plan = code == null ? null : BY_CODE.get(code);
        if (plan == null) {
            throw BusinessException.invalid("Plano de tarifa desconhecido: " + code);
        }
        return plan;
    }
}
