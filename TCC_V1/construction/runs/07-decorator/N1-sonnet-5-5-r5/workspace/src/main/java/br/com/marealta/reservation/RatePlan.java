package br.com.marealta.reservation;

import br.com.marealta.Money;

import java.math.BigDecimal;

public enum RatePlan {

    STANDARD("0", true),
    NON_REFUNDABLE("0.15", false);

    private final BigDecimal discountRate;
    private final boolean refundable;

    RatePlan(String discountRate, boolean refundable) {
        this.discountRate = new BigDecimal(discountRate);
        this.refundable = refundable;
    }

    public BigDecimal discountOn(BigDecimal subtotal) {
        return Money.round(subtotal.multiply(discountRate));
    }

    public BigDecimal refundOn(BigDecimal paid) {
        return refundable ? Money.round(paid) : Money.ZERO;
    }
}
