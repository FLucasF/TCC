package pousada.web;

import pousada.domain.Room;
import pousada.domain.RoomType;

import java.math.BigDecimal;

public record RoomView(int number, RoomType type, int capacity, BigDecimal nightlyRate) {
    static RoomView of(Room room) {
        return new RoomView(room.number(), room.type(), room.capacity(), room.nightlyRate());
    }
}
