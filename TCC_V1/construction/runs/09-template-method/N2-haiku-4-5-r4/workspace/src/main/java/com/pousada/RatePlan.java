package com.pousada;

public enum RatePlan {
    STANDARD,
    NON_REFUNDABLE;

    public static RatePlan fromString(String value) {
        return RatePlan.valueOf(value.toUpperCase());
    }
}
