package com.pousadamarealta.domain;

public class Room {
    private int number;
    private RoomType type;

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

    public String getTypeCode() {
        return type.getCode();
    }

    public int getCapacity() {
        return type.getCapacity();
    }

    public double getNightlyRate() {
        return type.getNightlyRate();
    }
}
