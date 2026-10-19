package com.pousadamarealta.web;

import com.pousadamarealta.domain.Room;

public class RoomResponse {
    private int number;
    private String type;
    private int capacity;
    private double nightlyRate;

    public RoomResponse(Room room) {
        this.number = room.getNumber();
        this.type = room.getType().name();
        this.capacity = room.getCapacity();
        this.nightlyRate = room.getNightlyRate().doubleValue();
    }

    public int getNumber() {
        return number;
    }

    public String getType() {
        return type;
    }

    public int getCapacity() {
        return capacity;
    }

    public double getNightlyRate() {
        return nightlyRate;
    }
}
