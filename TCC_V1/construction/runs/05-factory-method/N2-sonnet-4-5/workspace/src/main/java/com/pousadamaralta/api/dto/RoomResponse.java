package com.pousadamaralta.api.dto;

import com.pousadamaralta.domain.model.Room;
import java.math.BigDecimal;

public record RoomResponse(
    String number,
    String type,
    int capacity,
    BigDecimal nightlyRate
) {
    public static RoomResponse from(Room room) {
        return new RoomResponse(
            room.getNumber(),
            room.getType().name(),
            room.getCapacity(),
            room.getNightlyRate()
        );
    }
}
