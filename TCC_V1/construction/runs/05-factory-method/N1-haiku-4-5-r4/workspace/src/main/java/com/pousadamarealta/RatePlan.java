package com.pousadamarealta;

import com.fasterxml.jackson.annotation.JsonValue;

public enum RatePlan {
    STANDARD("STANDARD"),
    NON_REFUNDABLE("NON_REFUNDABLE");

    @JsonValue
    public final String value;

    RatePlan(String value) {
        this.value = value;
    }

    public static RatePlan fromString(String value) {
        return RatePlan.valueOf(value);
    }
}
