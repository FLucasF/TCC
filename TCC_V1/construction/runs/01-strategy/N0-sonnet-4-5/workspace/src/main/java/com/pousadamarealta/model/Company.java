package com.pousadamarealta.model;

import java.math.BigDecimal;

public enum Company {
    TECNOSUL(new BigDecimal("0.12")),
    AGROVALE(new BigDecimal("0.18")),
    FARMAVIDA(new BigDecimal("0.25"));

    private final BigDecimal discountRate;

    Company(BigDecimal discountRate) {
        this.discountRate = discountRate;
    }

    public BigDecimal getDiscountRate() {
        return discountRate;
    }

    public static Company fromCode(String code) {
        if (code == null) {
            return null;
        }
        try {
            return valueOf(code.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
