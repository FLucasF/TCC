package com.pousada.domain;

public enum RatePlan {
    STANDARD("STANDARD"),
    NON_REFUNDABLE("NON_REFUNDABLE");

    private final String displayName;

    RatePlan(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
