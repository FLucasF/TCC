package pousada.plan;

import pousada.domain.Money;

import java.math.BigDecimal;

public class StandardPlan implements RatePlan {
    @Override
    public String code() {
        return "STANDARD";
    }

    @Override
    public BigDecimal discount(Stay stay, BigDecimal subtotal) {
        return Money.ZERO;
    }

    @Override
    public BigDecimal refund(BigDecimal amountPaid) {
        return amountPaid;
    }
}
