package com.pousada.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pousada.models.Room;
import java.math.BigDecimal;

public record RoomResponse(
        @JsonProperty("number")
        int number,
        @JsonProperty("type")
        String type,
        @JsonProperty("capacity")
        int capacity,
        @JsonProperty("nightlyRate")
        BigDecimal nightlyRate
) {
    public static RoomResponse fromRoom(Room room) {
        return new RoomResponse(
                room.getNumber(),
                room.getType(),
                room.getCapacity(),
                room.getNightlyRate()
        );
    }
}
