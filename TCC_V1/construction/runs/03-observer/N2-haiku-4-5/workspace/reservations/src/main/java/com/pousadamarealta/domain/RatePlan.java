package com.pousadamarealta.domain;

public enum RatePlan {
    STANDARD("STANDARD"),
    NON_REFUNDABLE("NON_REFUNDABLE");

    private final String code;

    RatePlan(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static RatePlan fromCode(String code) {
        for (RatePlan plan : values()) {
            if (plan.code.equals(code)) {
                return plan;
            }
        }
        throw new IllegalArgumentException("Unknown rate plan: " + code);
    }
}
