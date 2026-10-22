package com.pousadamarealta.domain;

public enum MessageChannel {
    EMAIL("EMAIL");

    private final String code;

    MessageChannel(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
