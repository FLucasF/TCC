package com.pousada.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pousada.domain.Room;

import java.math.BigDecimal;

public record RoomResponse(
    @JsonProperty("number") String number,
    @JsonProperty("type") String type,
    @JsonProperty("capacity") int capacity,
    @JsonProperty("nightlyRate") BigDecimal nightlyRate
) {
    public static RoomResponse from(Room room) {
        return new RoomResponse(
            room.getNumber(),
            room.getType().name(),
            room.getCapacity(),
            room.getType().getNightlyRate()
        );
    }
}
