package com.clothing.model;

public enum ShippingMethod {
    ECONOMY,
    EXPRESS,
    STORE_PICKUP,
    COURIER;

    public static ShippingMethod fromString(String value) {
        if (value == null) return null;
        try {
            return ShippingMethod.valueOf(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
