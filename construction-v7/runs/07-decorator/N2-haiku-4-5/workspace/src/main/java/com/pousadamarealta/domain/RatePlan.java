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
}
