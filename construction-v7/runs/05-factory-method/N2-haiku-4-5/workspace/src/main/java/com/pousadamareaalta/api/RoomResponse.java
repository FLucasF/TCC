package com.pousadamareaalta.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pousadamareaalta.model.Room;

import java.math.BigDecimal;

public record RoomResponse(
    @JsonProperty("number") int number,
    @JsonProperty("type") String type,
    @JsonProperty("capacity") int capacity,
    @JsonProperty("nightlyRate") BigDecimal nightlyRate
) {
    public static RoomResponse from(Room room) {
        return new RoomResponse(
            room.number(),
            room.type().toString(),
            room.capacity(),
            room.nightlyRate()
        );
    }
}
