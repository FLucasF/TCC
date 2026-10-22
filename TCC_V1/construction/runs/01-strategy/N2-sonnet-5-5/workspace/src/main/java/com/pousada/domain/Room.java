package com.pousada.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

public record Room(int number, RoomType type, int capacity, BigDecimal nightlyRate) {

    public BigDecimal priceOfNight(LocalDate night) {
        return Season.isHigh(night) ? Money.percent(nightlyRate, "1.20") : nightlyRate;
    }
}
