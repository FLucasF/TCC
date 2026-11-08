package br.com.marealta.plan;

import br.com.marealta.ApiException;
import br.com.marealta.Money;

import java.math.BigDecimal;
import java.util.Map;

/** Convênio com empresas: desconto por empresa, identificada pelo código (nome em maiúsculas). */
public final class CorporatePlan implements RatePlan {
    private static final Map<String, BigDecimal> DISCOUNTS = Map.of(
            "TECNOSUL", new BigDecimal("0.12"),
            "AGROVALE", new BigDecimal("0.18"),
            "FARMAVIDA", new BigDecimal("0.25"));

    @Override public String code() { return "CORPORATE"; }

    @Override public Quote quote(PricingContext ctx) {
        String company = ctx.companyCode();
        BigDecimal rate = company == null ? null : DISCOUNTS.get(company.trim());
        if (rate == null) {
            throw ApiException.invalid("O plano CORPORATE exige um companyCode de empresa conveniada.");
        }
        BigDecimal subtotal = ctx.seasonalSubtotal();
        return new Quote(subtotal, Money.times(subtotal, rate));
    }

    @Override public BigDecimal refund(BigDecimal amountPaid) { return amountPaid; }
}
