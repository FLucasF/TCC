package com.pousadameraalta.api.dto;

import com.pousadameraalta.model.Room;
import java.math.BigDecimal;

public class RoomResponse {
    public int number;
    public String type;
    public int capacity;
    public BigDecimal nightlyRate;

    public RoomResponse(Room room) {
        this.number = room.getNumber();
        this.type = room.getType();
        this.capacity = room.getCapacity();
        this.nightlyRate = room.getNightlyRate();
    }
}
