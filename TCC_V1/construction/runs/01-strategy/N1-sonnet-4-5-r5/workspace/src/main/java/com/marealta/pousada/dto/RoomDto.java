package com.marealta.pousada.dto;

import com.marealta.pousada.domain.model.Room;

import java.math.BigDecimal;

public record RoomDto(
    int number,
    String type,
    int capacity,
    BigDecimal nightlyRate
) {
    public static RoomDto from(Room room) {
        return new RoomDto(
            room.getNumber(),
            room.getType().name(),
            room.getCapacity(),
            room.getType().getNightlyRate()
        );
    }
}
