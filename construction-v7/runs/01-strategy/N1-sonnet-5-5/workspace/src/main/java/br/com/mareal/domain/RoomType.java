package br.com.mareal.domain;

import java.math.BigDecimal;

public enum RoomType {
    STANDARD(2, "249.90"),
    DELUXE(3, "389.90"),
    SUITE(4, "579.90");

    public final int capacity;
    public final BigDecimal nightlyRate;

    RoomType(int capacity, String nightlyRate) {
        this.capacity = capacity;
        this.nightlyRate = Money.of(nightlyRate);
    }
}
