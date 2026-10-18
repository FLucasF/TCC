package com.loja.model;

public enum NotificationStatus {
    NEW, READ;

    public static NotificationStatus fromString(String value) {
        try {
            return NotificationStatus.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
