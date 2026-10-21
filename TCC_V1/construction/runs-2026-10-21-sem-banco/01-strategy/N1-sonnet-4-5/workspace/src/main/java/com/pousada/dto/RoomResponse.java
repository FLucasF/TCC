package com.pousada.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pousada.model.Room;
import com.pousada.model.RoomType;

import java.math.BigDecimal;

public class RoomResponse {
    @JsonProperty("number")
    public String number;

    @JsonProperty("type")
    public RoomType type;

    @JsonProperty("capacity")
    public int capacity;

    @JsonProperty("nightlyRate")
    public BigDecimal nightlyRate;

    public RoomResponse(Room room) {
        this.number = room.getNumber();
        this.type = room.getType();
        this.capacity = room.getCapacity();
        this.nightlyRate = room.getNightlyRate();
    }
}
