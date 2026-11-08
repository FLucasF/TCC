package com.pousada.api.dto;

import com.pousada.domain.Room;

public class RoomResponse {
    public int number;
    public String type;
    public int capacity;
    public double nightlyRate;

    public static RoomResponse from(Room room) {
        RoomResponse dto = new RoomResponse();
        dto.number = room.getNumber();
        dto.type = room.getType().toString();
        dto.capacity = room.getCapacity();
        dto.nightlyRate = room.getNightlyRate().doubleValue();
        return dto;
    }
}
