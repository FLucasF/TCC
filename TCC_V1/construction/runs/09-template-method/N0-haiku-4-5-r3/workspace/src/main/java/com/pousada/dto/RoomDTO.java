package com.pousada.dto;

import com.pousada.models.Room;
import java.math.BigDecimal;

public class RoomDTO {
    public int number;
    public String type;
    public int capacity;
    public BigDecimal nightlyRate;

    public RoomDTO(Room room) {
        this.number = room.number;
        this.type = room.type;
        this.capacity = room.capacity;
        this.nightlyRate = room.nightlyRate;
    }
}
