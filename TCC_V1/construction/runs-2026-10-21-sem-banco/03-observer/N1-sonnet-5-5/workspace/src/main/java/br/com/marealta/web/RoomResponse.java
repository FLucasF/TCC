package br.com.marealta.web;

import br.com.marealta.domain.Room;
import br.com.marealta.domain.RoomType;

import java.math.BigDecimal;

public record RoomResponse(int number, RoomType type, int capacity, BigDecimal nightlyRate) {

    static RoomResponse of(Room room) {
        return new RoomResponse(room.number(), room.type(), room.capacity(), room.nightlyRate());
    }
}
