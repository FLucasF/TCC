package com.loja.model;

public enum OrderStatus {
    AWAITING_PAYMENT, PAID, SHIPPED, DELIVERED, CANCELLED;

    public static OrderStatus fromString(String value) {
        try {
            return OrderStatus.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
