package com.pousadamarealta.model;

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
}
