package com.marealta;

import java.math.BigDecimal;

/** Plano de tarifa: define o desconto sobre as noites e se o pagamento é devolvido no cancelamento. */
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

    public BigDecimal refund(BigDecimal paid) {
        return refundable ? paid : Money.ZERO;
    }
}
