package com.pousada.model;

import java.math.BigDecimal;

public enum RatePlan {
    STANDARD("STANDARD", BigDecimal.ZERO),
    NON_REFUNDABLE("NON_REFUNDABLE", new BigDecimal("0.15"));

    private final String name;
    private final BigDecimal discountRate;

    RatePlan(String name, BigDecimal discountRate) {
        this.name = name;
        this.discountRate = discountRate;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getDiscountRate() {
        return discountRate;
    }

    public static RatePlan fromString(String value) {
        for (RatePlan plan : RatePlan.values()) {
            if (plan.name.equals(value)) {
                return plan;
            }
        }
        return null;
    }
}
