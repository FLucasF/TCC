package com.pousada.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pousada.domain.Room;
import com.pousada.domain.RoomType;

import java.math.BigDecimal;

public record RoomResponse(
    @JsonProperty("number") String number,
    @JsonProperty("type") RoomType type,
    @JsonProperty("capacity") int capacity,
    @JsonProperty("nightlyRate") BigDecimal nightlyRate
) {
    public static RoomResponse from(Room room) {
        return new RoomResponse(
            room.getNumber(),
            room.getType(),
            room.getCapacity(),
            room.getNightlyRate()
        );
    }
}
