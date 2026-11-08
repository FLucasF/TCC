package br.com.marealta.plan;

import br.com.marealta.DomainException;
import br.com.marealta.domain.Stay;

import java.math.BigDecimal;
import java.util.Map;

public class CorporatePlan extends NightlyPlan {
    private static final Map<String, BigDecimal> DISCOUNTS = Map.of(
            "TECNOSUL", new BigDecimal("0.12"),
            "AGROVALE", new BigDecimal("0.18"),
            "FARMAVIDA", new BigDecimal("0.25"));

    @Override public String code() { return "CORPORATE"; }

    @Override
    protected void validate(Stay stay) {
        if (stay.companyCode() == null || !DISCOUNTS.containsKey(stay.companyCode())) {
            throw DomainException.invalid("O plano CORPORATE exige um companyCode de empresa conveniada");
        }
    }

    @Override protected BigDecimal discountRate(Stay stay) { return DISCOUNTS.get(stay.companyCode()); }
    @Override public BigDecimal refund(BigDecimal amountPaid) { return amountPaid; }
}
