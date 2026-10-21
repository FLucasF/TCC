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

    /** Preço da noite que começa em {@code night}, já com a alta temporada. */
    public BigDecimal priceOf(LocalDate night) {
        return Formats.round(nightlyRate().multiply(Season.multiplier(night)));
    }
}
