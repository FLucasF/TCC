package com.pousadamarealta.controller.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pousadamarealta.model.Room;
import com.pousadamarealta.model.RoomType;

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
        response.nightlyRate = room.getNightlyRate().doubleValue();
        return response;
    }

    public String getNumber() {
        return number;
    }

    public RoomType getType() {
        return type;
    }

    public int getCapacity() {
        return capacity;
    }

    @JsonProperty("nightlyRate")
    public double getNightlyRate() {
        return nightlyRate;
    }
}
