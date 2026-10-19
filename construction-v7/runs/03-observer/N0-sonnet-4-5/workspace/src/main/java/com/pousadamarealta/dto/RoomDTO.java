package com.pousadamarealta.dto;

import com.pousadamarealta.domain.Room;
import com.pousadamarealta.domain.RoomType;

import java.math.BigDecimal;

public record RoomDTO(
    String number,
    RoomType type,
    int capacity,
    BigDecimal nightlyRate
) {
    public static RoomDTO from(Room room) {
        return new RoomDTO(
            room.getNumber(),
            room.getType(),
            room.getCapacity(),
            room.getNightlyRate()
        );
    }
}
