package com.pousadamareal.api;

import com.pousadamareal.model.Room;
import java.math.BigDecimal;

public class RoomResponse {
    private final String number;
    private final String type;
    private final int capacity;
    private final BigDecimal nightlyRate;

    public RoomResponse(Room room) {
        this.number = room.getNumber();
        this.type = room.getType().name();
        this.capacity = room.getCapacity();
        this.nightlyRate = room.getNightlyRate();
    }

    public String getNumber() { return number; }
    public String getType() { return type; }
    public int getCapacity() { return capacity; }
    public BigDecimal getNightlyRate() { return nightlyRate; }
}
