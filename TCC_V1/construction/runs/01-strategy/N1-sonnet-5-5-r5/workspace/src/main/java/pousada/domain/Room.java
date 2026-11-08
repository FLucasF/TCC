package pousada.domain;

import java.math.BigDecimal;

public record Room(int number, RoomType type, int capacity, BigDecimal nightlyRate) {
    public Room(int number, RoomType type) {
        this(number, type, type.capacity(), type.nightlyRate());
    }
}
