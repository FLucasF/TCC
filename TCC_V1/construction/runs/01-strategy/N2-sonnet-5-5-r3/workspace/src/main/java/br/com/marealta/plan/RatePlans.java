package br.com.marealta.plan;

import br.com.marealta.DomainException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Registro dos planos de tarifa disponíveis. */
public class RatePlans {
    private final Map<String, RatePlan> byCode = new LinkedHashMap<>();

    public RatePlans(List<RatePlan> plans) {
        plans.forEach(p -> byCode.put(p.code(), p));
    }

    public RatePlan get(String code) {
        RatePlan plan = byCode.get(code);
        if (plan == null) throw DomainException.invalid("ratePlan inválido: " + code);
        return plan;
    }
}
