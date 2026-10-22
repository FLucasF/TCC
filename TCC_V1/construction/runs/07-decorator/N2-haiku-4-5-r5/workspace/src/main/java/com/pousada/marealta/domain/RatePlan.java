package com.pousada.marealta.domain;

public enum RatePlan {
    STANDARD("STANDARD"),
    NON_REFUNDABLE("NON_REFUNDABLE");

    private final String value;

    RatePlan(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static RatePlan fromString(String value) {
        for (RatePlan plan : RatePlan.values()) {
            if (plan.value.equals(value)) {
                return plan;
            }
        }
        throw new IllegalArgumentException("Unknown rate plan: " + value);
    }
}
