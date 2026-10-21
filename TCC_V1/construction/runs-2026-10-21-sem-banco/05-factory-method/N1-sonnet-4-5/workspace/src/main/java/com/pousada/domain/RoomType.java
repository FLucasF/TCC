package com.pousada.domain;

import java.math.BigDecimal;

public enum RoomType {
    STANDARD(new BigDecimal("249.90"), 2),
    DELUXE(new BigDecimal("389.90"), 3),
    SUITE(new BigDecimal("579.90"), 4);

    private final BigDecimal nightlyRate;
    private final int capacity;

    RoomType(BigDecimal nightlyRate, int capacity) {
        this.nightlyRate = nightlyRate;
        this.capacity = capacity;
    }

    public BigDecimal getNightlyRate() {
        return nightlyRate;
    }

    public int getCapacity() {
        return capacity;
    }
}
