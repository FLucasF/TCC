package pousada.domain;

import java.math.BigDecimal;

public enum RatePlan {
    STANDARD("0.00", true),
    NON_REFUNDABLE("0.15", false);

    private final String discountRate;
    private final boolean refundable;

    RatePlan(String discountRate, boolean refundable) {
        this.discountRate = discountRate;
        this.refundable = refundable;
    }

    public BigDecimal discountOn(BigDecimal subtotal) {
        return Money.times(subtotal, discountRate);
    }

    public BigDecimal refundFor(BigDecimal amountPaid) {
        return refundable ? amountPaid : Money.ZERO;
    }
}
