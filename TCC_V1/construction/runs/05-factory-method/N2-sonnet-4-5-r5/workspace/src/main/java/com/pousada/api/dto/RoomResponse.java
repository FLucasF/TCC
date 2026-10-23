package com.pousada.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pousada.domain.model.Room;

import java.math.BigDecimal;

public record RoomResponse(
    @JsonProperty("number") String number,
    @JsonProperty("type") String type,
    @JsonProperty("capacity") int capacity,
    @JsonProperty("nightlyRate") BigDecimal nightlyRate
) {
    public static RoomResponse from(Room room) {
        return new RoomResponse(
            room.number(),
            room.type().name(),
            room.capacity(),
            room.nightlyRate()
        );
    }
}
