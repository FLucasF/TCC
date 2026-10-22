package com.pousada.plan;

import com.pousada.domain.Money;
import com.pousada.error.ApiException;

import java.math.BigDecimal;
import java.util.Map;

public class CorporatePlan implements RatePlan {

    private static final Map<String, String> DISCOUNT_BY_COMPANY = Map.of(
            "TECNOSUL", "0.12",
            "AGROVALE", "0.18",
            "FARMAVIDA", "0.25");

    @Override
    public String code() {
        return "CORPORATE";
    }

    @Override
    public void validate(PricingRequest request) {
        String company = request.companyCode();
        if (company == null || !DISCOUNT_BY_COMPANY.containsKey(company)) {
            throw ApiException.invalidRequest("O convênio exige o código de uma empresa conveniada");
        }
    }

    @Override
    public BigDecimal discount(PricingRequest request, BigDecimal subtotal) {
        return Money.percent(subtotal, DISCOUNT_BY_COMPANY.get(request.companyCode()));
    }
}
