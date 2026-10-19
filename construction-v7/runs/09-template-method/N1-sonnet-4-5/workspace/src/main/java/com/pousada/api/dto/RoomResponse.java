package com.pousada.api.dto;

import com.pousada.domain.model.Room;
import com.pousada.domain.model.RoomType;

import java.math.BigDecimal;

public record RoomResponse(
    String number,
    RoomType type,
    int capacity,
    BigDecimal nightlyRate
) {
    public static RoomResponse from(Room room) {
        return new RoomResponse(
            room.getNumber(),
            room.getType(),
            room.getCapacity(),
            room.getType().getNightlyRate()
        );
    }
}
