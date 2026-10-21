package com.pousadamarealta.domain;

public enum RoomType {
    STANDARD("STANDARD", 249.90, 2),
    DELUXE("DELUXE", 389.90, 3),
    SUITE("SUITE", 579.90, 4);

    private final String name;
    private final double nightlyRate;
    private final int capacity;

    RoomType(String name, double nightlyRate, int capacity) {
        this.name = name;
        this.nightlyRate = nightlyRate;
        this.capacity = capacity;
    }

    public String getName() {
        return name;
    }

    public double getNightlyRate() {
        return nightlyRate;
    }

    public int getCapacity() {
        return capacity;
    }

    public static RoomType fromName(String name) {
        for (RoomType type : RoomType.values()) {
            if (type.name.equals(name)) {
                return type;
            }
        }
        return null;
    }
}
