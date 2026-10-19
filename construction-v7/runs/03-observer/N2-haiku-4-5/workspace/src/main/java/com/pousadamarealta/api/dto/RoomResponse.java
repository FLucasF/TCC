package com.pousadamarealta.api.dto;

import com.pousadamarealta.domain.Room;
import java.math.BigDecimal;

public record RoomResponse(
    int number,
    String type,
    int capacity,
    BigDecimal nightlyRate
) {
    public static RoomResponse from(Room room) {
        return new RoomResponse(
            room.number(),
            room.type().displayName,
            room.capacity(),
            room.nightly()
        );
    }
}
