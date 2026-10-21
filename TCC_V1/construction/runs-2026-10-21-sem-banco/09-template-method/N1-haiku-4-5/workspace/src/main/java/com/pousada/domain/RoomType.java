package com.pousada.domain;

import java.math.BigDecimal;

public enum RoomType {
    STANDARD("STANDARD", 2, new BigDecimal("249.90")),
    DELUXE("DELUXE", 3, new BigDecimal("389.90")),
    SUITE("SUITE", 4, new BigDecimal("579.90"));

    private final String name;
    private final int capacity;
    private final BigDecimal nightlyRate;

    RoomType(String name, int capacity, BigDecimal nightlyRate) {
        this.name = name;
        this.capacity = capacity;
        this.nightlyRate = nightlyRate;
    }

    public String getDisplayName() {
        return name;
    }

    public int getCapacity() {
        return capacity;
    }

    public BigDecimal getNightlyRate() {
        return nightlyRate;
    }
}
