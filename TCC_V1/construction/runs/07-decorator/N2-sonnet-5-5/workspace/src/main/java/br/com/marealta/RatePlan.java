package br.com.marealta;

import java.math.BigDecimal;

public enum RatePlan {
    STANDARD("0") {
        @Override BigDecimal refundFor(BigDecimal paid) { return paid; }
    },
    NON_REFUNDABLE("0.15") {
        @Override BigDecimal refundFor(BigDecimal paid) { return Money.ZERO; }
    };

    private final BigDecimal discountRate;

    RatePlan(String discountRate) {
        this.discountRate = new BigDecimal(discountRate);
    }

    BigDecimal discountOn(BigDecimal subtotal) {
        return Money.round(subtotal.multiply(discountRate));
    }

    /** O que volta ao hóspede quando uma reserva com este valor pago é cancelada. */
    abstract BigDecimal refundFor(BigDecimal paid);
}
