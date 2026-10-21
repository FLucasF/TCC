package pousada;

import java.math.BigDecimal;

public record Room(int number, RoomType type) {
    public int capacity() { return type.capacity; }
    public BigDecimal nightlyRate() { return type.nightlyRate; }
}
