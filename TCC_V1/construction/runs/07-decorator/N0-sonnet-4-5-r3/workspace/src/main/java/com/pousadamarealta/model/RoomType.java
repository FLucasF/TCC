package com.pousadamarealta.model;

public enum RoomType {
    STANDARD(2, 249.90),
    DELUXE(3, 389.90),
    SUITE(4, 579.90);

    private final int capacity;
    private final double nightlyRate;

    RoomType(int capacity, double nightlyRate) {
        this.capacity = capacity;
        this.nightlyRate = nightlyRate;
    }

    public int getCapacity() {
        return capacity;
    }

    public double getNightlyRate() {
        return nightlyRate;
    }
}
