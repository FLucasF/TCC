package com.pousadamarealta.model;

public class Room {
    private final int number;
    private final RoomType type;
    private final int capacity;
    private final double nightlyRate;

    public Room(int number, RoomType type, int capacity, double nightlyRate) {
        this.number = number;
        this.type = type;
        this.capacity = capacity;
        this.nightlyRate = nightlyRate;
    }

    public int getNumber() {
        return number;
    }

    public RoomType getType() {
        return type;
    }

    public int getCapacity() {
        return capacity;
    }

    public double getNightlyRate() {
        return nightlyRate;
    }
}
