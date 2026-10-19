package com.pousadamarealta.dto.response;

import com.pousadamarealta.domain.Room;
import com.pousadamarealta.domain.RoomType;

import java.math.BigDecimal;

public class RoomResponse {
    private final String number;
    private final RoomType type;
    private final int capacity;
    private final BigDecimal nightlyRate;

    public RoomResponse(Room room) {
        this.number = room.getNumber();
        this.type = room.getType();
        this.capacity = room.getCapacity();
        this.nightlyRate = room.getNightlyRate();
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
