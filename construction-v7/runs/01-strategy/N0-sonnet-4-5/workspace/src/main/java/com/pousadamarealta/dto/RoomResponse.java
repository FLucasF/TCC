package com.pousadamarealta.dto;

import com.pousadamarealta.model.Room;
import com.pousadamarealta.model.RoomType;

import java.math.BigDecimal;

public class RoomResponse {
    private String number;
    private RoomType type;
    private int capacity;
    private BigDecimal nightlyRate;

    public RoomResponse(Room room) {
        this.number = room.getNumber();
        this.type = room.getType();
        this.capacity = room.getCapacity();
        this.nightlyRate = room.getType().getNightlyRate();
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

    public BigDecimal getNightlyRate() {
        return nightlyRate;
    }
}
