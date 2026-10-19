package com.pousada.strategy;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class NonRefundableRatePlan implements RatePlanStrategy {
    private static final BigDecimal DISCOUNT_RATE = new BigDecimal("0.15");

    @Override
    public BigDecimal calculateDiscount(BigDecimal subtotal) {
        return subtotal.multiply(DISCOUNT_RATE).setScale(2, RoundingMode.HALF_EVEN);
    }

    @Override
    public boolean isRefundable() {
        return false;
    }
}
