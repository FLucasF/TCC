package com.marealta.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;

/** Preço de uma estadia: noites, soma das noites, desconto e total. */
public record Quote(int nights, BigDecimal subtotal, BigDecimal discount, BigDecimal total) {
    private static final BigDecimal HIGH_SEASON_FACTOR = new BigDecimal("1.20");

    public static Quote of(Room room, LocalDate checkIn, LocalDate checkOut, RatePlan plan) {
        BigDecimal subtotal = Money.ZERO;
        int nights = 0;
        for (LocalDate night = checkIn; night.isBefore(checkOut); night = night.plusDays(1)) {
            subtotal = subtotal.add(nightPrice(room, night));
            nights++;
        }
        BigDecimal discount = plan.discount(subtotal);
        return new Quote(nights, subtotal, discount, Money.of(subtotal.subtract(discount)));
    }

    private static BigDecimal nightPrice(Room room, LocalDate night) {
        if (!isHighSeason(night)) {
            return room.nightlyRate();
        }
        return Money.of(room.nightlyRate().multiply(HIGH_SEASON_FACTOR));
    }

    /** De 15 de dezembro ao fim de fevereiro, e julho inteiro. */
    static boolean isHighSeason(LocalDate night) {
        Month month = night.getMonth();
        return month == Month.JULY
                || month == Month.JANUARY
                || month == Month.FEBRUARY
                || (month == Month.DECEMBER && night.getDayOfMonth() >= 15);
    }
}
