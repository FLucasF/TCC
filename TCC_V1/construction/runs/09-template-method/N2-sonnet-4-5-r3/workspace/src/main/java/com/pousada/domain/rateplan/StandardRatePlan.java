package com.pousada.domain.rateplan;

import java.math.BigDecimal;

public class StandardRatePlan implements RatePlan {
    @Override
    public String getName() {
        return "STANDARD";
    }

    @Override
    public BigDecimal calculateDiscount(BigDecimal subtotal) {
        return BigDecimal.ZERO;
    }

    @Override
    public BigDecimal calculateRefund(BigDecimal amountPaid) {
        return amountPaid;
    }
}
