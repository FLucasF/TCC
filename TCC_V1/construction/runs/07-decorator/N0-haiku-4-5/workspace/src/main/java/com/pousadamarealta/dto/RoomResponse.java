package com.pousadamarealta.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pousadamarealta.model.Room;

public class RoomResponse {
    @JsonProperty("number")
    public int number;

    @JsonProperty("type")
    public String type;

    @JsonProperty("capacity")
    public int capacity;

    @JsonProperty("nightlyRate")
    public double nightlyRate;

    public static RoomResponse from(Room room) {
        RoomResponse response = new RoomResponse();
        response.number = room.getNumber();
        response.type = room.getType().toString();
        response.capacity = room.getCapacity();
        response.nightlyRate = room.getNightlyRate();
        return response;
    }
}
