package marealta;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Catálogo dos planos de tarifa. Para um plano novo, acrescente-o aqui. */
public class RatePlans {
    private final Map<String, RatePlan> byCode = new LinkedHashMap<>();

    public RatePlans(List<RatePlan> plans) {
        plans.forEach(plan -> byCode.put(plan.code(), plan));
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
}
