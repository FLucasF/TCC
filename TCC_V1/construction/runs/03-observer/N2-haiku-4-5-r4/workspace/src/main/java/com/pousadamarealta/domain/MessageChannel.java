package com.pousadamarealta.domain;

public enum MessageChannel {
    EMAIL("EMAIL");

    private final String value;

    MessageChannel(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
