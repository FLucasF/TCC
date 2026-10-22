package com.pousada.marealta.domain;

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

    public String getName() {
        return name;
    }

    public int getCapacity() {
        return capacity;
    }

    public BigDecimal getNightlyRate() {
        return nightlyRate;
    }

    public static RoomType fromString(String value) {
        for (RoomType type : RoomType.values()) {
            if (type.name.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown room type: " + value);
    }
}
