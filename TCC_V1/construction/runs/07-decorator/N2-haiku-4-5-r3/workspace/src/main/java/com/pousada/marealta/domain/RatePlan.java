package com.pousada.marealta.domain;

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
}
