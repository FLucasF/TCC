package com.pousada.marealta.model;

import java.math.BigDecimal;

public enum RoomType {
    STANDARD("STANDARD", 2, new BigDecimal("249.90")),
    DELUXE("DELUXE", 3, new BigDecimal("389.90")),
    SUITE("SUITE", 4, new BigDecimal("579.90"));

    public final String name;
    public final int capacity;
    public final BigDecimal nightlyRate;

    RoomType(String name, int capacity, BigDecimal nightlyRate) {
        this.name = name;
        this.capacity = capacity;
        this.nightlyRate = nightlyRate;
    }
}
