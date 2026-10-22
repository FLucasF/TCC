package com.pousada.marealta.domain;

public enum MessageStatus {
    SENT,
    BLOCKED;

    public static MessageStatus fromString(String value) {
        return MessageStatus.valueOf(value);
    }
}
