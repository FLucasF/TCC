package com.pousada.marealba.model;

public enum RatePlan {
    STANDARD(0.0),
    NON_REFUNDABLE(0.15);

    private final double discount;

    RatePlan(double discount) {
        this.discount = discount;
    }

    public double getDiscount() {
        return discount;
    }
}
