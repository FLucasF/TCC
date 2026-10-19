package com.pousadamarealta.model;

public class Room {
    private final String number;
    private final RoomType type;

    public Room(String number, RoomType type) {
        this.number = number;
        this.type = type;
    }

    public String getNumber() {
        return number;
    }

    public RoomType getType() {
        return type;
    }

    public int getCapacity() {
        return type.getCapacity();
    }
}
