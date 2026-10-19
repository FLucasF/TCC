package br.com.marealta.domain;

import java.math.BigDecimal;

/** Plano de tarifa: define o desconto sobre a soma das noites e a política de reembolso. */
public enum RatePlan {
    STANDARD("0", true),
    NON_REFUNDABLE("0.15", false);

    private final BigDecimal discountRate;
    private final boolean refundable;

    RatePlan(String discountRate, boolean refundable) {
        this.discountRate = new BigDecimal(discountRate);
        this.refundable = refundable;
    }

    public BigDecimal discount(BigDecimal subtotal) {
        return Money.round(subtotal.multiply(discountRate));
    }

    public BigDecimal refund(BigDecimal amountPaid) {
        return refundable ? amountPaid : Money.ZERO;
    }
}
