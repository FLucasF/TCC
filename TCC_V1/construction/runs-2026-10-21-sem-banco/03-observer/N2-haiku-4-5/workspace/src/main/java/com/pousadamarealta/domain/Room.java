package com.pousadamarealta.domain;

public class Room {
    private final int number;
    private final RoomType type;

    public Room(int number, RoomType type) {
        this.number = number;
        this.type = type;
    }

    public int getNumber() {
        return number;
    }

    public RoomType getType() {
        return type;
    }

    public int getCapacity() {
        return type.getCapacity();
    }

    public double getNightlyRate() {
        return type.getNightlyRate();
    }
}
