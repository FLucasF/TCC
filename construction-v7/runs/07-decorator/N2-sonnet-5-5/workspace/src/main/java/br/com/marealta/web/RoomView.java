package br.com.marealta.web;

import br.com.marealta.domain.Room;

import java.math.BigDecimal;

public record RoomView(int number, String type, int capacity, BigDecimal nightlyRate) {
    public static RoomView of(Room room) {
        return new RoomView(room.number(), room.type().name(), room.capacity(), room.nightlyRate());
    }
}
