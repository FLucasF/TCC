package com.pousada.dto;

import com.pousada.model.Room;

import java.math.BigDecimal;

public class RoomDTO {
    public int number;
    public String type;
    public int capacity;
    public BigDecimal nightlyRate;

    public static RoomDTO from(Room room) {
        RoomDTO dto = new RoomDTO();
        dto.number = room.getNumber();
        dto.type = room.getType().toString();
        dto.capacity = room.getCapacity();
        dto.nightlyRate = room.getNightlyRate();
        return dto;
    }
}
