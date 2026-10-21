package com.pousadamaretalta;

import java.math.BigDecimal;

public enum RatePlan {
    STANDARD("0.00", true),
    NON_REFUNDABLE("0.15", false);

    public final BigDecimal discountRate;
    public final boolean refundable;

    RatePlan(String discountRate, boolean refundable) {
        this.discountRate = new BigDecimal(discountRate);
        this.refundable = refundable;
    }
}
