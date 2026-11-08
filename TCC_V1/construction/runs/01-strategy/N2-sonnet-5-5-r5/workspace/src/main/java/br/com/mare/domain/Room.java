package br.com.mare.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

public record Room(int number, RoomType type, int capacity, BigDecimal nightlyRate) {
    private static final BigDecimal HIGH_SEASON_FACTOR = new BigDecimal("1.20");

    public BigDecimal priceOn(LocalDate night) {
        return HighSeason.includes(night) ? Money.round(nightlyRate.multiply(HIGH_SEASON_FACTOR)) : nightlyRate;
    }
}
