package com.loja.model;

public enum ShippingMethod {
    ECONOMY, EXPRESS, STORE_PICKUP, COURIER;

    public static ShippingMethod fromString(String value) {
        try {
            return ShippingMethod.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
