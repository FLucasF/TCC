package br.com.marealta.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;

/** Preço de uma estadia: noites, soma das noites, desconto e total. */
public record Quote(int nights, BigDecimal subtotal, BigDecimal discount, BigDecimal total) {
    private static final BigDecimal HIGH_SEASON_FACTOR = new BigDecimal("1.20");

    public static Quote of(Room room, LocalDate checkIn, LocalDate checkOut, RatePlan plan) {
        int nights = 0;
        BigDecimal subtotal = Money.ZERO;
        for (LocalDate night = checkIn; night.isBefore(checkOut); night = night.plusDays(1)) {
            subtotal = subtotal.add(nightPrice(room, night));
            nights++;
        }
        BigDecimal discount = plan.discountOn(subtotal);
        return new Quote(nights, subtotal, discount, Money.round(subtotal.subtract(discount)));
    }

    static BigDecimal nightPrice(Room room, LocalDate night) {
        BigDecimal rate = room.nightlyRate();
        return Money.round(isHighSeason(night) ? rate.multiply(HIGH_SEASON_FACTOR) : rate);
    }

    static boolean isHighSeason(LocalDate night) {
        Month month = night.getMonth();
        return month == Month.JANUARY || month == Month.FEBRUARY || month == Month.JULY
                || (month == Month.DECEMBER && night.getDayOfMonth() >= 15);
    }
}
