package com.pousada.marealta.domain;

public enum MessageStatus {
    SENT,
    BLOCKED;

    public static MessageStatus fromString(String name) {
        for (MessageStatus status : MessageStatus.values()) {
            if (status.name().equals(name)) {
                return status;
            }
        }
        return null;
    }
}
