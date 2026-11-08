package com.marealta.plan;

import com.marealta.ApiException;
import java.util.Map;

class CorporatePlan extends PerNightPlan {
    private static final Map<String, String> DISCOUNTS = Map.of(
            "TECNOSUL", "0.12",
            "AGROVALE", "0.18",
            "FARMAVIDA", "0.25");

    @Override public String code() { return "CORPORATE"; }

    @Override protected String discountRate(BookingTerms t) {
        String company = t.companyCode() == null ? null : t.companyCode().trim();
        if (company == null || company.isEmpty()) {
            throw ApiException.invalid("O plano CORPORATE exige o companyCode da empresa");
        }
        String rate = DISCOUNTS.get(company);
        if (rate == null) {
            throw ApiException.invalid("Empresa sem convênio: " + company);
        }
        return rate;
    }
}
