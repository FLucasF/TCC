package br.com.marealta.pousada.web;

import java.math.BigDecimal;

import br.com.marealta.pousada.domain.Room;
import br.com.marealta.pousada.domain.RoomType;

public record RoomView(int number, RoomType type, int capacity, BigDecimal nightlyRate) {

    public static RoomView of(Room room) {
        return new RoomView(room.number(), room.type(), room.capacity(), room.nightlyRate());
    }
}
