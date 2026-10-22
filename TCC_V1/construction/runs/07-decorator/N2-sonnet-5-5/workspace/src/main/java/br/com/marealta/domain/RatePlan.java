package br.com.marealta.domain;

import java.math.BigDecimal;

public enum RatePlan {
    STANDARD(0, true),
    NON_REFUNDABLE(15, false);

    private final int discountPercent;
    private final boolean refundable;

    RatePlan(int discountPercent, boolean refundable) {
        this.discountPercent = discountPercent;
        this.refundable = refundable;
    }

    public BigDecimal discountOn(BigDecimal subtotal) {
        return Money.round(subtotal.multiply(BigDecimal.valueOf(discountPercent)).movePointLeft(2));
    }

    public BigDecimal refundOf(BigDecimal amountPaid) {
        return refundable ? amountPaid : Money.ZERO;
    }
}
