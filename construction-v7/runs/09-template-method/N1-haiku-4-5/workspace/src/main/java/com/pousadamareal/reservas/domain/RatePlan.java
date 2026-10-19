package com.pousadamareal.reservas.domain;

import com.fasterxml.jackson.annotation.JsonValue;

public enum RatePlan {
    STANDARD("STANDARD"),
    NON_REFUNDABLE("NON_REFUNDABLE");

    private final String value;

    RatePlan(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    public static RatePlan fromString(String value) {
        for (RatePlan plan : RatePlan.values()) {
            if (plan.value.equals(value)) {
                return plan;
            }
        }
        throw new IllegalArgumentException("Invalid RatePlan: " + value);
    }
}
