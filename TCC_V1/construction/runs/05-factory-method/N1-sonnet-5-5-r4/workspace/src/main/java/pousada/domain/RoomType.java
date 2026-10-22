package pousada.domain;

import java.math.BigDecimal;

public enum RoomType {
    STANDARD(2, Money.of("249.90")),
    DELUXE(3, Money.of("389.90")),
    SUITE(4, Money.of("579.90"));

    private final int capacity;
    private final BigDecimal nightlyRate;

    RoomType(int capacity, BigDecimal nightlyRate) {
        this.capacity = capacity;
        this.nightlyRate = nightlyRate;
    }

    public int capacity() {
        return capacity;
    }

    public BigDecimal nightlyRate() {
        return nightlyRate;
    }
}
