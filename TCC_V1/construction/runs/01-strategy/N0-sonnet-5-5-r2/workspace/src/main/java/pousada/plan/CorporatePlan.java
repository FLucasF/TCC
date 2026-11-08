package pousada.plan;

import pousada.ApiException;

import java.math.BigDecimal;
import java.util.Map;

/** Convênio com empresas: desconto nas diárias conforme o código da empresa. */
public class CorporatePlan implements RatePlan {
    private static final Map<String, BigDecimal> COMPANIES = Map.of(
            "TECNOSUL", new BigDecimal("0.12"),
            "AGROVALE", new BigDecimal("0.18"),
            "FARMAVIDA", new BigDecimal("0.25"));

    public String code() { return "CORPORATE"; }

    @Override
    public void validate(BookingContext ctx) {
        if (ctx.companyCode() == null || !COMPANIES.containsKey(ctx.companyCode())) {
            throw ApiException.invalid("O plano CORPORATE exige um companyCode válido (TECNOSUL, AGROVALE ou FARMAVIDA)");
        }
    }

    public Quote quote(BookingContext ctx) {
        return Quote.withDiscountRate(ctx.seasonalSubtotal(), COMPANIES.get(ctx.companyCode()));
    }

    public BigDecimal refund(BigDecimal amountPaid) { return amountPaid; }
}
