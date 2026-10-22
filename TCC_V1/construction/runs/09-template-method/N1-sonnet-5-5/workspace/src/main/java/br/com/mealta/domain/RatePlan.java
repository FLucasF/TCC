package br.com.mealta.domain;

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

    /** Quanto volta ao hóspede se a reserva for cancelada tendo pago {@code paid}. */
    public BigDecimal refundOf(BigDecimal paid) {
        return refundable ? paid : Money.ZERO;
    }
}
