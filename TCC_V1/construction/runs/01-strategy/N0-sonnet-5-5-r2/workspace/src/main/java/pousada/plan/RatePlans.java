package pousada.plan;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Registro dos planos de tarifa disponíveis. Plano novo = nova classe + uma linha aqui. */
public final class RatePlans {
    private static final List<RatePlan> ALL = List.of(
            new StandardPlan(),
            new NonRefundablePlan(),
            new CorporatePlan(),
            new WeekendPackagePlan(),
            new LongStayPlan());

    private static final Map<String, RatePlan> BY_CODE =
            ALL.stream().collect(Collectors.toMap(RatePlan::code, Function.identity()));

    private RatePlans() {}

    public static Optional<RatePlan> find(String code) {
        return Optional.ofNullable(BY_CODE.get(code));
    }

    public static RatePlan get(String code) {
        return find(code).orElseThrow(() -> new IllegalStateException("Plano desconhecido: " + code));
    }
}
