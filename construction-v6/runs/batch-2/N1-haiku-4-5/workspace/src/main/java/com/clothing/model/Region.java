package com.clothing.model;

import java.math.BigDecimal;

public enum Region {
    SOUTHEAST(new BigDecimal("0.01"), null),
    SOUTH(new BigDecimal("0.01"), null),
    MIDWEST(new BigDecimal("0.015"), null),
    NORTH(new BigDecimal("0.025"), new BigDecimal("30.00")),
    NORTHEAST(new BigDecimal("0.02"), new BigDecimal("30.00"));

    public final BigDecimal insuranceRate;
    public final BigDecimal insuranceLimit;

    Region(BigDecimal insuranceRate, BigDecimal insuranceLimit) {
        this.insuranceRate = insuranceRate;
        this.insuranceLimit = insuranceLimit;
    }

    public static Region fromString(String value) {
        if (value == null) return null;
        try {
            return Region.valueOf(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public int getExtraDeliveryDays() {
        return switch (this) {
            case NORTH -> 3;
            case NORTHEAST -> 2;
            default -> 0;
        };
    }
}
