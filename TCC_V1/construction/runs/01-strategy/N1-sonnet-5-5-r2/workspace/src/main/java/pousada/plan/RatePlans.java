package pousada.plan;

import pousada.domain.AppException;
import pousada.domain.ErrorCode;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Todos os planos de tarifa da pousada. Plano novo entra só nesta lista. */
public final class RatePlans {
    private static final List<RatePlan> ALL = List.of(
            new StandardPlan(),
            new NonRefundablePlan(),
            new CorporatePlan(),
            new WeekendPackagePlan(),
            new LongStayPlan());

    private static final Map<String, RatePlan> BY_CODE =
            ALL.stream().collect(Collectors.toMap(RatePlan::code, Function.identity()));

    private RatePlans() {
    }

    public static RatePlan byCode(String code) {
        RatePlan plan = code == null ? null : BY_CODE.get(code);
        if (plan == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Plano de tarifa desconhecido: " + code);
        }
        return plan;
    }
}
