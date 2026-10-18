package com.loja.model;

public enum NotificationType {
    PAYMENT_APPROVED, ORDER_SHIPPED, ORDER_CANCELLED;

    public static NotificationType fromString(String value) {
        try {
            return NotificationType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
