package com.pousadamaralta.domain.model;

import java.math.BigDecimal;

public enum RoomType {
    STANDARD(2, new BigDecimal("249.90")),
    DELUXE(3, new BigDecimal("389.90")),
    SUITE(4, new BigDecimal("579.90"));

    private final int capacity;
    private final BigDecimal nightlyRate;

    RoomType(int capacity, BigDecimal nightlyRate) {
        this.capacity = capacity;
        this.nightlyRate = nightlyRate;
    }

    public int getCapacity() {
        return capacity;
    }

    public BigDecimal getNightlyRate() {
        return nightlyRate;
    }
}
