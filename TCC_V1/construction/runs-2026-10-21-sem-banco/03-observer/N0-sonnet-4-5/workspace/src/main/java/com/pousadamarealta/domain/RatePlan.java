package com.pousadamarealta.domain;

import java.math.BigDecimal;

public enum RatePlan {
    STANDARD(BigDecimal.ZERO, true),
    NON_REFUNDABLE(new BigDecimal("0.15"), false);

    private final BigDecimal discountRate;
    private final boolean refundable;

    RatePlan(BigDecimal discountRate, boolean refundable) {
        this.discountRate = discountRate;
        this.refundable = refundable;
    }

    public BigDecimal getDiscountRate() {
        return discountRate;
    }

    public boolean isRefundable() {
        return refundable;
    }
}
