package com.pousada.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pousada.domain.Room;
import java.math.BigDecimal;

public class RoomResponse {
    @JsonProperty("number")
    public Integer number;

    @JsonProperty("type")
    public String type;

    @JsonProperty("capacity")
    public Integer capacity;

    @JsonProperty("nightlyRate")
    public BigDecimal nightlyRate;

    public static RoomResponse fromRoom(Room room) {
        RoomResponse dto = new RoomResponse();
        dto.number = room.getNumber();
        dto.type = room.getType();
        dto.capacity = room.getCapacity();
        dto.nightlyRate = room.getNightlyRate();
        return dto;
    }
}
