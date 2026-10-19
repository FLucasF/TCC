package pousada;

import java.math.BigDecimal;

public record RoomView(int number, RoomType type, int capacity, BigDecimal nightlyRate) {

    public static RoomView of(Room room) {
        return new RoomView(room.number(), room.type(), room.type().capacity, room.type().nightlyRate);
    }
}
