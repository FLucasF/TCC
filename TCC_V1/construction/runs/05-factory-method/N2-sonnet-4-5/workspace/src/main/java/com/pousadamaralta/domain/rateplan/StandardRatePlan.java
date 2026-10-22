package com.pousadamaralta.domain.rateplan;

import java.math.BigDecimal;

public class StandardRatePlan implements RatePlan {

    @Override
    public BigDecimal calculateDiscount(BigDecimal subtotal) {
        return BigDecimal.ZERO;
    }

    @Override
    public BigDecimal calculateRefund(BigDecimal amountPaid, BigDecimal total) {
        return amountPaid;
    }
}
