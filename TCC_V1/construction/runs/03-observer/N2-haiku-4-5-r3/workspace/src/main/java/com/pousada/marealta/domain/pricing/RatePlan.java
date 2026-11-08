package com.pousada.marealta.domain.pricing;

public enum RatePlan {
    STANDARD("STANDARD", 0),
    NON_REFUNDABLE("NON_REFUNDABLE", 15);

    private final String displayName;
    private final int discountPercent;

    RatePlan(String displayName, int discountPercent) {
        this.displayName = displayName;
        this.discountPercent = discountPercent;
    }

    public String displayName() {
        return displayName;
    }

    public int discountPercent() {
        return discountPercent;
    }

    public static RatePlan fromString(String value) {
        for (RatePlan plan : RatePlan.values()) {
            if (plan.displayName.equals(value)) {
                return plan;
            }
        }
        throw new IllegalArgumentException("Unknown rate plan: " + value);
    }
}
