package com.pousadamarealta.dto;

import com.pousadamarealta.domain.Room;
import com.pousadamarealta.domain.RoomType;

import java.math.BigDecimal;

public record RoomDto(
    String number,
    RoomType type,
    int capacity,
    BigDecimal nightlyRate
) {
    public static RoomDto from(Room room) {
        return new RoomDto(
            room.number(),
            room.type(),
            room.capacity(),
            room.type().getNightlyRate()
        );
    }
}
