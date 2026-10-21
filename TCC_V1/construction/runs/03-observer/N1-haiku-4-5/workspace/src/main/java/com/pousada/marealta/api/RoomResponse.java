package com.pousada.marealta.api;

import java.math.BigDecimal;
import com.pousada.marealta.model.Room;

public class RoomResponse {
    public int number;
    public String type;
    public int capacity;
    public BigDecimal nightlyRate;

    public RoomResponse(Room room) {
        this.number = room.number;
        this.type = room.type.name;
        this.capacity = room.getCapacity();
        this.nightlyRate = room.getNightlyRate();
    }
}
