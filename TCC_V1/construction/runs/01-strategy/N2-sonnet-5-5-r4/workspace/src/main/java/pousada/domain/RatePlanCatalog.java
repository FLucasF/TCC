package pousada.domain;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class RatePlanCatalog {
    private final Map<String, RatePlan> byCode = new LinkedHashMap<>();

    public RatePlanCatalog(List<RatePlan> plans) {
        plans.forEach(plan -> byCode.put(plan.code(), plan));
    }

    public Optional<RatePlan> find(String code) {
        return Optional.ofNullable(byCode.get(code));
    }
}
