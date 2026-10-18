package com.clothing.model;

public enum NotificationType {
    PAYMENT_APPROVED,
    ORDER_SHIPPED,
    ORDER_CANCELLED;

    public static NotificationType fromString(String value) {
        if (value == null) return null;
        try {
            return NotificationType.valueOf(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
