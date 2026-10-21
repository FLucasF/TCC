package com.pousada.dto;

import com.pousada.domain.Room;
import com.pousada.domain.RoomType;

public class RoomResponse {
    private String number;
    private RoomType type;
    private int capacity;
    private double nightlyRate;

    public static RoomResponse from(Room room) {
        RoomResponse response = new RoomResponse();
        response.number = room.getNumber();
        response.type = room.getType();
        response.capacity = room.getCapacity();
        response.nightlyRate = room.getNightlyRate().getAmount().doubleValue();
        return response;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public RoomType getType() {
        return type;
    }

    public void setType(RoomType type) {
        this.type = type;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public double getNightlyRate() {
        return nightlyRate;
    }

    public void setNightlyRate(double nightlyRate) {
        this.nightlyRate = nightlyRate;
    }
}
