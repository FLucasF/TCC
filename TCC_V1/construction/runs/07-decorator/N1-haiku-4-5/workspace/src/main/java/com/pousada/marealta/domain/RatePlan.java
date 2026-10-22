package com.pousada.marealta.domain;

public enum RatePlan {
    STANDARD(0.0),
    NON_REFUNDABLE(0.15);

    private final double discountRate;

    RatePlan(double discountRate) {
        this.discountRate = discountRate;
    }

    public double getDiscountRate() {
        return discountRate;
    }

    public static RatePlan fromString(String name) {
        for (RatePlan plan : RatePlan.values()) {
            if (plan.name().equals(name)) {
                return plan;
            }
        }
        return null;
    }
}
