package pousada.plan;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** The only place that knows which rate plans exist. A new plan is one class and one line here. */
public final class RatePlans {
    private static final Map<String, RatePlan> BY_CODE = new LinkedHashMap<>();

    static {
        register(new StandardPlan());
        register(new NonRefundablePlan());
        register(new CorporatePlan());
        register(new WeekendPackagePlan());
        register(new LongStayPlan());
    }

    private RatePlans() {}

    private static void register(RatePlan plan) {
        BY_CODE.put(plan.code(), plan);
    }

    public static Optional<RatePlan> find(String code) {
        return Optional.ofNullable(BY_CODE.get(code));
    }
}
