package br.com.mareal.plans;

import br.com.mareal.domain.ApiException;
import br.com.mareal.domain.Season;
import java.math.BigDecimal;
import java.util.Map;

public class CorporatePlan implements RatePlan {
    private static final Map<String, BigDecimal> DISCOUNT_BY_COMPANY = Map.of(
            "TECNOSUL", new BigDecimal("0.12"),
            "AGROVALE", new BigDecimal("0.18"),
            "FARMAVIDA", new BigDecimal("0.25"));

    public String code() {
        return "CORPORATE";
    }

    public Pricing price(Stay stay) {
        BigDecimal rate = stay.companyCode() == null ? null : DISCOUNT_BY_COMPANY.get(stay.companyCode());
        if (rate == null) {
            throw ApiException.invalid("O plano CORPORATE exige o código de uma empresa conveniada (companyCode).");
        }
        return Pricing.withDiscount(Season.sumOfNights(stay.roomType(), stay.checkIn(), stay.checkOut()), rate);
    }

    public BigDecimal refund(BigDecimal amountPaid) {
        return amountPaid;
    }
}
