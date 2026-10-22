package br.com.marealta.web.dto;

import br.com.marealta.domain.Room;

import java.math.BigDecimal;

public record RoomDTO(
    String number,
    String type,
    int capacity,
    BigDecimal nightlyRate
) {
    public static RoomDTO from(Room room) {
        return new RoomDTO(
            room.getNumber(),
            room.getType().name(),
            room.getCapacity(),
            room.getNightlyRate().amount()
        );
    }
}
