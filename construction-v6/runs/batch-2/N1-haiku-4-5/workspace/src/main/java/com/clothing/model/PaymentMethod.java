package com.clothing.model;

public enum PaymentMethod {
    PIX,
    CREDIT_CARD,
    BANK_SLIP;

    public static PaymentMethod fromString(String value) {
        if (value == null) return null;
        try {
            return PaymentMethod.valueOf(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
