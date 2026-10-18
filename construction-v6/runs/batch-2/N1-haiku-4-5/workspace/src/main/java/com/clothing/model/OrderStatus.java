package com.clothing.model;

public enum OrderStatus {
    AWAITING_PAYMENT,
    PAID,
    SHIPPED,
    DELIVERED,
    CANCELLED;

    public static OrderStatus fromString(String value) {
        if (value == null) return null;
        try {
            return OrderStatus.valueOf(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
