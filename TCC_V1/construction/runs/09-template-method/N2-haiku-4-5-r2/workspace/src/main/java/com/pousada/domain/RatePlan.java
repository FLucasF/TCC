package com.pousada.domain;

public enum RatePlan {
    STANDARD("STANDARD", 0.0),
    NON_REFUNDABLE("NON_REFUNDABLE", 0.15);

    private final String displayName;
    private final double discountPercentage;

    RatePlan(String displayName, double discountPercentage) {
        this.displayName = displayName;
        this.discountPercentage = discountPercentage;
    }

    public String getDisplayName() {
        return displayName;
    }

    public double getDiscountPercentage() {
        return discountPercentage;
    }
}
