package com.pousadamareaalta.dto;

import com.pousadamareaalta.model.Room;

public class RoomDTO {
    public int number;
    public String type;
    public int capacity;
    public double nightlyRate;

    public static RoomDTO fromRoom(Room room) {
        RoomDTO dto = new RoomDTO();
        dto.number = room.getNumber();
        dto.type = room.getType().toString();
        dto.capacity = room.getCapacity();
        dto.nightlyRate = room.getNightlyRate().doubleValue();
        return dto;
    }
}
