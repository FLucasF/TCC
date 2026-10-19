package com.pousadamarealta.domain;

public enum MessageStatus {
    SENT("SENT"),
    BLOCKED("BLOCKED");

    private final String code;

    MessageStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
