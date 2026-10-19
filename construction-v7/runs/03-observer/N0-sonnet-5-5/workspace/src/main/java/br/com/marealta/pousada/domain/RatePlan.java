package br.com.marealta.pousada.domain;

import java.math.BigDecimal;

public enum RatePlan {
    STANDARD("0.00", true),
    NON_REFUNDABLE("0.15", false);

    private final BigDecimal discountRate;
    private final boolean refundable;

    RatePlan(String discountRate, boolean refundable) {
        this.discountRate = new BigDecimal(discountRate);
        this.refundable = refundable;
    }

    public BigDecimal discountRate() {
        return discountRate;
    }

    public boolean refundable() {
        return refundable;
    }
}
