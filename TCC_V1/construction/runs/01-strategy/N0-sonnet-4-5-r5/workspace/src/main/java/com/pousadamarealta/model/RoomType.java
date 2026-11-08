package com.pousadamarealta.model;

import java.math.BigDecimal;

public enum RoomType {
    STANDARD(2, new BigDecimal("249.90"), new BigDecimal("520.00")),
    DELUXE(3, new BigDecimal("389.90"), new BigDecimal("820.00")),
    SUITE(4, new BigDecimal("579.90"), new BigDecimal("1200.00"));

    private final int capacity;
    private final BigDecimal nightlyRate;
    private final BigDecimal weekendPackagePrice;

    RoomType(int capacity, BigDecimal nightlyRate, BigDecimal weekendPackagePrice) {
        this.capacity = capacity;
        this.nightlyRate = nightlyRate;
        this.weekendPackagePrice = weekendPackagePrice;
    }

    public int getCapacity() {
        return capacity;
    }

    public BigDecimal getNightlyRate() {
        return nightlyRate;
    }

    public BigDecimal getWeekendPackagePrice() {
        return weekendPackagePrice;
    }
}
