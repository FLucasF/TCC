package com.pousadamarealta.api;

import com.pousadamarealta.domain.Room;
import com.pousadamarealta.domain.RoomType;

import java.math.BigDecimal;

public record RoomResponse(
    String number,
    RoomType type,
    int capacity,
    BigDecimal nightlyRate
) {
    public static RoomResponse from(Room room) {
        return new RoomResponse(
            room.number(),
            room.type(),
            room.capacity(),
            room.nightlyRate()
        );
    }
}
