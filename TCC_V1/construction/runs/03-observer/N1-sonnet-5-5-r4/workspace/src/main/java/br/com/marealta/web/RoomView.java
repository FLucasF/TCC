package br.com.marealta.web;

import br.com.marealta.domain.Room;
import br.com.marealta.domain.RoomType;

import java.math.BigDecimal;

record RoomView(int number, RoomType type, int capacity, BigDecimal nightlyRate) {
    static RoomView of(Room room) {
        return new RoomView(room.number(), room.type(), room.capacity(), room.nightlyRate());
    }
}
