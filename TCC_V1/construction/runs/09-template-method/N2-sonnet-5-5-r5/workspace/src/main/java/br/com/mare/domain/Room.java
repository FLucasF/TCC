package br.com.mare.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

public record Room(int number, RoomType type) {

    public int capacity() {
        return type.capacity();
    }

    public BigDecimal nightlyRate() {
        return type.nightlyRate();
    }

    /** Preço da noite que começa em {@code night}, já com o acréscimo da alta temporada. */
    public BigDecimal priceOfNight(LocalDate night) {
        return Season.priceOf(nightlyRate(), night);
    }
}
