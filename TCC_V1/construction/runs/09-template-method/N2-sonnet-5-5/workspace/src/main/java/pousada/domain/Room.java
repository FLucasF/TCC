package pousada.domain;

import java.math.BigDecimal;

public record Room(int number, RoomType type, int capacity, BigDecimal nightlyRate) {
    public static Room of(int number, RoomType type) {
        return new Room(number, type, type.capacity(), type.nightlyRate());
    }
}
