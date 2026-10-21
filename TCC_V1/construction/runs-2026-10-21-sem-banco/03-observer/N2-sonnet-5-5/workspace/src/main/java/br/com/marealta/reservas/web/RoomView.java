package br.com.marealta.reservas.web;

import br.com.marealta.reservas.domain.Room;
import br.com.marealta.reservas.domain.RoomType;

import java.math.BigDecimal;

public record RoomView(int number, RoomType type, int capacity, BigDecimal nightlyRate) {

    static RoomView of(Room room) {
        return new RoomView(room.number(), room.type(), room.capacity(), room.nightlyRate());
    }
}
