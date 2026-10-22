package com.pousada.models;

public enum RatePlan {
    STANDARD("STANDARD"),
    NON_REFUNDABLE("NON_REFUNDABLE");

    private final String label;

    RatePlan(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static RatePlan fromLabel(String label) {
        for (RatePlan plan : RatePlan.values()) {
            if (plan.label.equals(label)) {
                return plan;
            }
        }
        throw new IllegalArgumentException("Unknown rate plan: " + label);
    }
}
