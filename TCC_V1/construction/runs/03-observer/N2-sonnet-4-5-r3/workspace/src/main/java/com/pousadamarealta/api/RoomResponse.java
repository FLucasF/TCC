package com.pousadamarealta.api;

import com.pousadamarealta.domain.Room;

public record RoomResponse(
    int number,
    String type,
    int capacity,
    double nightlyRate
) {
    public static RoomResponse from(Room room) {
        return new RoomResponse(
            room.number(),
            room.type().name(),
            room.capacity(),
            room.nightlyRate().doubleValue()
        );
    }
}
