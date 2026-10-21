package com.pousadamarealta.domain;

public enum RoomType {
    STANDARD("STANDARD", 2, 249.90),
    DELUXE("DELUXE", 3, 389.90),
    SUITE("SUITE", 4, 579.90);

    public final String name;
    public final int capacity;
    public final double nightlyRate;

    RoomType(String name, int capacity, double nightlyRate) {
        this.name = name;
        this.capacity = capacity;
        this.nightlyRate = nightlyRate;
    }
}
