package com.pousada.marealta.infrastructure.web.dto;

import com.pousada.marealta.domain.room.Room;

public class RoomDTO {
    public int number;
    public String type;
    public int capacity;
    public double nightlyRate;

    public static RoomDTO fromDomain(Room room) {
        RoomDTO dto = new RoomDTO();
        dto.number = room.number();
        dto.type = room.type().displayName();
        dto.capacity = room.capacity();
        dto.nightlyRate = room.nightlyRate().doubleValue();
        return dto;
    }
}
