package br.com.marealta.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

public record Room(int number, RoomType type) {
    public int capacity() {
        return type.capacity();
    }

    public BigDecimal nightlyRate() {
        return type.nightlyRate();
    }

    /** Preço da noite que começa em {@code night}, com o acréscimo de alta temporada quando for o caso. */
    public BigDecimal priceOfNight(LocalDate night) {
        return isHighSeason(night) ? Money.fraction(nightlyRate(), new BigDecimal("1.20")) : nightlyRate();
    }

    /** Alta temporada: 15/12 até o fim de fevereiro, e julho inteiro. */
    static boolean isHighSeason(LocalDate date) {
        int month = date.getMonthValue();
        return month == 1 || month == 2 || month == 7 || (month == 12 && date.getDayOfMonth() >= 15);
    }
}
