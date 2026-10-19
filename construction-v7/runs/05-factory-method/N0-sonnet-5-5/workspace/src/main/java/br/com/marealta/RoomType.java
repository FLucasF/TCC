package br.com.marealta;

import java.math.BigDecimal;

enum RoomType {
    STANDARD(2, Money.of("249.90")),
    DELUXE(3, Money.of("389.90")),
    SUITE(4, Money.of("579.90"));

    final int capacity;
    final BigDecimal nightlyRate;

    RoomType(int capacity, BigDecimal nightlyRate) {
        this.capacity = capacity;
        this.nightlyRate = nightlyRate;
    }
}
