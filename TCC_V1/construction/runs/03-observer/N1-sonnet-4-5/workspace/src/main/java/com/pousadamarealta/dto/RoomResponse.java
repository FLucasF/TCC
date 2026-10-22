package com.pousadamarealta.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pousadamarealta.domain.model.Room;

import java.math.BigDecimal;

public record RoomResponse(
    String number,
    String type,
    int capacity,
    @JsonProperty("nightlyRate") BigDecimal nightlyRate
) {
    public static RoomResponse from(Room room) {
        return new RoomResponse(
            room.getNumber(),
            room.getType().name(),
            room.getCapacity(),
            room.getNightlyRate()
        );
    }
}
