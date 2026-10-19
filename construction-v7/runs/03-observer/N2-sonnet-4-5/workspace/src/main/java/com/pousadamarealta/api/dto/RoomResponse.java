package com.pousadamarealta.api.dto;

import com.pousadamarealta.model.Room;

import java.math.BigDecimal;

public class RoomResponse {
    private String number;
    private String type;
    private int capacity;
    private BigDecimal nightlyRate;

    public static RoomResponse from(Room room) {
        RoomResponse response = new RoomResponse();
        response.number = room.getNumber();
        response.type = room.getType().name();
        response.capacity = room.getCapacity();
        response.nightlyRate = room.getNightlyRate();
        return response;
    }

    public String getNumber() {
        return number;
    }

    public String getType() {
        return type;
    }

    public int getCapacity() {
        return capacity;
    }

    public BigDecimal getNightlyRate() {
        return nightlyRate;
    }
}
