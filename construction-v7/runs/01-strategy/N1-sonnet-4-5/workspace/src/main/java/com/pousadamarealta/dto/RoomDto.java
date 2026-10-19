package com.pousadamarealta.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pousadamarealta.model.Room;
import com.pousadamarealta.model.RoomType;

import java.math.BigDecimal;

public record RoomDto(
        @JsonProperty("number") int number,
        @JsonProperty("type") RoomType type,
        @JsonProperty("capacity") int capacity,
        @JsonProperty("nightlyRate") BigDecimal nightlyRate
) {
    public static RoomDto from(Room room) {
        return new RoomDto(
                room.getNumber(),
                room.getType(),
                room.getCapacity(),
                room.getNightlyRate()
        );
    }
}
