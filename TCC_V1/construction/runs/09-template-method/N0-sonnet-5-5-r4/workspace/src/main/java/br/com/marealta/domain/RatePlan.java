package br.com.marealta.domain;

import java.math.BigDecimal;

public enum RatePlan {
    STANDARD(new BigDecimal("0.00"), true),
    NON_REFUNDABLE(new BigDecimal("0.15"), false);

    private final BigDecimal discountRate;
    private final boolean refundable;

    RatePlan(BigDecimal discountRate, boolean refundable) {
        this.discountRate = discountRate;
        this.refundable = refundable;
    }

    /** Discount applied over the sum of the nights. */
    public BigDecimal discountOn(BigDecimal subtotal) {
        return Money.round(subtotal.multiply(discountRate));
    }

    /** What goes back to the guest when a reservation with this plan is cancelled. */
    public BigDecimal refundFor(BigDecimal amountPaid) {
        return refundable ? amountPaid : Money.ZERO;
    }
}
