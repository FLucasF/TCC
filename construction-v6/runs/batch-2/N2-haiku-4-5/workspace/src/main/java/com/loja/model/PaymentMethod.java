package com.loja.model;

public enum PaymentMethod {
    PIX, CREDIT_CARD, BANK_SLIP;

    public static PaymentMethod fromString(String value) {
        try {
            return PaymentMethod.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
