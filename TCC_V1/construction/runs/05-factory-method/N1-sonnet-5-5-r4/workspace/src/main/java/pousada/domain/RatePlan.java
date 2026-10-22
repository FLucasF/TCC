package pousada.domain;

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

    public BigDecimal discountOn(BigDecimal subtotal) {
        return Money.round(subtotal.multiply(discountRate));
    }

    public BigDecimal refundOf(BigDecimal amountPaid) {
        return refundable ? amountPaid : Money.ZERO;
    }
}
