package br.com.mareal.plans;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** The catalog of rate plans. A new plan is registered here, and nowhere else. */
public final class RatePlans {
    private static final Map<String, RatePlan> BY_CODE = new LinkedHashMap<>();

    static {
        List.of(new StandardPlan(), new NonRefundablePlan(), new CorporatePlan(),
                new WeekendPackagePlan(), new LongStayPlan())
                .forEach(plan -> BY_CODE.put(plan.code(), plan));
    }

    private RatePlans() {}

    public static Optional<RatePlan> find(String code) {
        return Optional.ofNullable(BY_CODE.get(code));
    }
}
