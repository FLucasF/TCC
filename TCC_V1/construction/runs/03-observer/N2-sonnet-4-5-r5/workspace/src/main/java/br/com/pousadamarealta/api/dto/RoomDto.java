package br.com.pousadamarealta.api.dto;

import br.com.pousadamarealta.domain.Room;
import java.math.BigDecimal;

public record RoomDto(String number, String type, int capacity, BigDecimal nightlyRate) {
    public static RoomDto from(Room room) {
        return new RoomDto(
            room.number(),
            room.type().name(),
            room.capacity(),
            room.nightlyRate()
        );
    }
}
