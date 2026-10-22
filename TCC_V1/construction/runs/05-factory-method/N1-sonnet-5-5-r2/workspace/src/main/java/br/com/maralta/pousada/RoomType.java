package br.com.maralta.pousada;

import java.math.BigDecimal;

public enum RoomType {
    STANDARD(2, "249.90"),
    DELUXE(3, "389.90"),
    SUITE(4, "579.90");

    private final int capacity;
    private final BigDecimal nightlyRate;

    RoomType(int capacity, String nightlyRate) {
        this.capacity = capacity;
        this.nightlyRate = Money.of(nightlyRate);
    }

    public int capacity() {
        return capacity;
    }

    public BigDecimal nightlyRate() {
        return nightlyRate;
    }
}
