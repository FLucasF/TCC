package com.pousada.dto;

import com.google.gson.annotations.SerializedName;
import com.pousada.domain.Room;

public class RoomResponse {
    @SerializedName("number")
    public int number;

    @SerializedName("type")
    public String type;

    @SerializedName("capacity")
    public int capacity;

    @SerializedName("nightlyRate")
    public double nightlyRate;

    public static RoomResponse fromRoom(Room room) {
        RoomResponse response = new RoomResponse();
        response.number = room.number;
        response.type = room.type.name;
        response.capacity = room.type.capacity;
        response.nightlyRate = room.type.nightlyRate;
        return response;
    }
}
