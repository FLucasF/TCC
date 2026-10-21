package com.pousadamarealta.dto;

import com.pousadamarealta.domain.Room;
import com.pousadamarealta.domain.RoomType;

import java.math.BigDecimal;

public record RoomResponse(
    int number,
    RoomType type,
    int capacity,
    BigDecimal nightlyRate
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
