package com.pousadamarealta.api.dto;

import com.pousadamarealta.domain.Room;
import com.pousadamarealta.domain.RoomType;

public record RoomResponse(
    String number,
    RoomType type,
    int capacity,
    double nightlyRate
) {
    public static RoomResponse from(Room room) {
        return new RoomResponse(
            room.getNumber(),
            room.getType(),
            room.getCapacity(),
            room.getNightlyRate().doubleValue()
        );
    }
}
