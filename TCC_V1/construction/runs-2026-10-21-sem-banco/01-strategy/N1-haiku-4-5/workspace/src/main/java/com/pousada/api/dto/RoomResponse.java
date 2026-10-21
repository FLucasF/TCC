package com.pousada.api.dto;

import com.pousada.domain.Room;

public class RoomResponse {
    public int number;
    public String type;
    public int capacity;
    public double nightlyRate;

    public static RoomResponse fromRoom(Room room) {
        RoomResponse response = new RoomResponse();
        response.number = room.getNumber();
        response.type = room.getType().getName();
        response.capacity = room.getCapacity();
        response.nightlyRate = room.getNightlyRate();
        return response;
    }
}
