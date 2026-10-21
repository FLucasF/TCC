package br.com.pousadamarealta.reservas.domain;

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

    public BigDecimal discount(BigDecimal subtotal) {
        return Money.round(subtotal.multiply(discountRate));
    }

    /** O que volta ao hóspede num cancelamento, dado o que ele pagou. */
    public BigDecimal refund(BigDecimal paid) {
        return refundable ? paid : Money.ZERO;
    }
}
