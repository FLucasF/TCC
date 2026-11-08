package pousada.domain;

import java.math.BigDecimal;
import java.util.Map;

/** Convênio com empresas: desconto nas diárias conforme o código da empresa. */
class CorporatePlan implements RatePlan {
    private static final Map<String, Integer> DISCOUNT_PERCENT_BY_COMPANY = Map.of(
            "TECNOSUL", 12,
            "AGROVALE", 18,
            "FARMAVIDA", 25);

    @Override
    public String code() {
        return "CORPORATE";
    }

    @Override
    public void validate(Booking booking) {
        if (booking.companyCode() == null || !DISCOUNT_PERCENT_BY_COMPANY.containsKey(booking.companyCode())) {
            throw DomainException.invalidRequest("O plano CORPORATE exige o código de uma empresa conveniada.");
        }
    }

    @Override
    public BigDecimal discount(Booking booking, BigDecimal subtotal) {
        return Money.percent(subtotal, DISCOUNT_PERCENT_BY_COMPANY.get(booking.companyCode()));
    }

    @Override
    public BigDecimal refund(BigDecimal amountPaid) {
        return amountPaid;
    }
}
