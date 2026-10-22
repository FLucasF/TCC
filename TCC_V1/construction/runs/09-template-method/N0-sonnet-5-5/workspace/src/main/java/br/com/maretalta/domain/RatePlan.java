package br.com.maretalta.domain;

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

    public BigDecimal discountRate() { return discountRate; }
    public boolean refundable() { return refundable; }
}
