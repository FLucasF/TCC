package pousada.plan;

import pousada.domain.Money;

import java.math.BigDecimal;

public class NonRefundablePlan implements RatePlan {
    @Override
    public String code() {
        return "NON_REFUNDABLE";
    }

    @Override
    public BigDecimal discount(Stay stay, BigDecimal subtotal) {
        return Money.fraction(subtotal, "0.15");
    }

    @Override
    public BigDecimal refund(BigDecimal amountPaid) {
        return Money.ZERO;
    }
}
