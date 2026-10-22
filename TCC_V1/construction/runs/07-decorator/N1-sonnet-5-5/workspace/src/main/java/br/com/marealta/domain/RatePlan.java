package br.com.marealta.domain;

import java.math.BigDecimal;

/** Planos de tarifa: cada plano traz o seu desconto e a sua regra de reembolso. */
public enum RatePlan {
    STANDARD("0.00", true),
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

    public BigDecimal refundFor(BigDecimal amountPaid) {
        return refundable ? Money.round(amountPaid) : Money.ZERO;
    }
}
