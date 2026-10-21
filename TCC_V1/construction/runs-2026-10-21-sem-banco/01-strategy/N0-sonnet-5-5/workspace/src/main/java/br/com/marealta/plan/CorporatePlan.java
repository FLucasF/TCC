package br.com.marealta.plan;

import br.com.marealta.domain.Money;
import br.com.marealta.domain.Quote;
import java.math.BigDecimal;
import java.util.Map;

/** Convênio com empresas: desconto nas diárias conforme o código da empresa. */
public class CorporatePlan implements RatePlan {
    private static final Map<String, Integer> DISCOUNT_PERCENT_BY_COMPANY = Map.of(
            "TECNOSUL", 12,
            "AGROVALE", 18,
            "FARMAVIDA", 25);

    public String code() { return "CORPORATE"; }

    public Quote quote(BookingRequest r) {
        Integer percent = r.companyCode() == null ? null : DISCOUNT_PERCENT_BY_COMPANY.get(r.companyCode());
        if (percent == null) {
            throw new PlanRuleException("Plano CORPORATE exige um companyCode de empresa conveniada");
        }
        return NightlyPricing.withPercentDiscount(r, percent);
    }

    public BigDecimal refund(BigDecimal amountPaid) { return Money.round(amountPaid); }
}
