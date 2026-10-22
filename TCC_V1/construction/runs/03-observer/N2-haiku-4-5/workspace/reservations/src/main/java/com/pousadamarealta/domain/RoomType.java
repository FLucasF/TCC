package com.pousadamarealta.domain;

public enum RoomType {
    STANDARD("STANDARD", 249.90, 2),
    DELUXE("DELUXE", 389.90, 3),
    SUITE("SUITE", 579.90, 4);

    private final String code;
    private final double nightlyRate;
    private final int capacity;

    RoomType(String code, double nightlyRate, int capacity) {
        this.code = code;
        this.nightlyRate = nightlyRate;
        this.capacity = capacity;
    }

    public String getCode() {
        return code;
    }

    public double getNightlyRate() {
        return nightlyRate;
    }

    public int getCapacity() {
        return capacity;
    }

    public static RoomType fromCode(String code) {
        for (RoomType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown room type: " + code);
    }
}
