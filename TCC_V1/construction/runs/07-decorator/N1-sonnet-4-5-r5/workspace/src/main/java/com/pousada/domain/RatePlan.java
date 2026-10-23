package com.pousada.domain;

import java.math.BigDecimal;

public enum RatePlan {
    STANDARD(BigDecimal.ZERO),
    NON_REFUNDABLE(new BigDecimal("0.15"));

    private final BigDecimal discountRate;

    RatePlan(BigDecimal discountRate) {
        this.discountRate = discountRate;
    }

    public BigDecimal getDiscountRate() {
        return discountRate;
    }

    public boolean isRefundable() {
        return this == STANDARD;
    }
}
