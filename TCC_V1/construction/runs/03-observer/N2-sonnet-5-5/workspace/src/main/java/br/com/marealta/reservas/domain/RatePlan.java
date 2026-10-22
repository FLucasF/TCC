package br.com.marealta.reservas.domain;

import java.math.BigDecimal;

/** Plano de tarifa: o desconto sobre a soma das noites e o que volta ao hóspede no cancelamento. */
public enum RatePlan {
    STANDARD("0.00", true),
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
