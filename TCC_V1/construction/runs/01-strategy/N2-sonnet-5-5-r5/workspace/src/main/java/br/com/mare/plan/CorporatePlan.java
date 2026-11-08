package br.com.mare.plan;

import br.com.mare.domain.ApiException;

import java.math.BigDecimal;
import java.util.Map;

class CorporatePlan extends NightlyRatePlan {
    private static final Map<String, BigDecimal> DISCOUNTS = Map.of(
            "TECNOSUL", new BigDecimal("0.12"),
            "AGROVALE", new BigDecimal("0.18"),
            "FARMAVIDA", new BigDecimal("0.25"));

    @Override
    public String code() { return "CORPORATE"; }

    @Override
    protected void validate(Booking booking) {
        if (booking.companyCode() == null || !DISCOUNTS.containsKey(booking.companyCode())) {
            throw ApiException.invalid("O plano CORPORATE exige um companyCode de empresa conveniada");
        }
    }

    @Override
    protected BigDecimal discountRate(Booking booking) { return DISCOUNTS.get(booking.companyCode()); }
}
