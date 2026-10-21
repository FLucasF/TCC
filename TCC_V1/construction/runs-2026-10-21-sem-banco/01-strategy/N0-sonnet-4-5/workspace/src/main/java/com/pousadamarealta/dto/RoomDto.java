package com.pousadamarealta.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pousadamarealta.model.Room;

import java.math.BigDecimal;

public record RoomDto(
        @JsonProperty("number") String number,
        @JsonProperty("type") String type,
        @JsonProperty("capacity") int capacity,
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
