package com.pousada.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pousada.domain.Room;

import java.math.BigDecimal;

public record RoomDto(
    String number,
    String type,
    int capacity,
    @JsonProperty("nightlyRate") BigDecimal nightlyRate
) {
    public static RoomDto from(Room room) {
        return new RoomDto(
            room.getNumber(),
            room.getType().name(),
            room.getCapacity(),
            room.getNightlyRate()
        );
    }
}
