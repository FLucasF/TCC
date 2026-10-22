package com.pousada.maralta.controller.dto;

import com.pousada.maralta.domain.Room;

import java.math.BigDecimal;

public record RoomResponse(
    String number,
    String type,
    int capacity,
    BigDecimal nightlyRate
) {
    public static RoomResponse from(Room room) {
        return new RoomResponse(
            room.number(),
            room.type().name(),
            room.type().getCapacity(),
            room.type().getNightlyRate()
        );
    }
}
