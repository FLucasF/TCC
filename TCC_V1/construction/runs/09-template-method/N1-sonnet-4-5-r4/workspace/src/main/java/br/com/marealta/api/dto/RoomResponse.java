package br.com.marealta.api.dto;

import br.com.marealta.domain.Room;
import br.com.marealta.domain.RoomType;

import java.math.BigDecimal;

public record RoomResponse(
    String number,
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
