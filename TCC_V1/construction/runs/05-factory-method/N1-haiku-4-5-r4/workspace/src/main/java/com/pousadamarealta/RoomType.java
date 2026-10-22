package com.pousadamarealta;

public enum RoomType {
    STANDARD("STANDARD", 2, 249.90),
    DELUXE("DELUXE", 3, 389.90),
    SUITE("SUITE", 4, 579.90);

    public final String type;
    public final int capacity;
    public final double nightlyRate;

    RoomType(String type, int capacity, double nightlyRate) {
        this.type = type;
        this.capacity = capacity;
        this.nightlyRate = nightlyRate;
    }
}
