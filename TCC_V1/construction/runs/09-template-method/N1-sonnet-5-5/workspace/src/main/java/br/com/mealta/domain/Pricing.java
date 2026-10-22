package br.com.mealta.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;

public class Pricing {
    private static final BigDecimal HIGH_SEASON_FACTOR = new BigDecimal("1.20");

    public Quote quote(Room room, LocalDate checkIn, LocalDate checkOut, RatePlan plan) {
        BigDecimal subtotal = Money.ZERO;
        int nights = 0;
        for (LocalDate night = checkIn; night.isBefore(checkOut); night = night.plusDays(1)) {
            subtotal = subtotal.add(nightPrice(room, night));
            nights++;
        }
        BigDecimal discount = plan.discountOn(subtotal);
        return new Quote(nights, subtotal, discount, Money.round(subtotal.subtract(discount)));
    }

    public BigDecimal nightPrice(Room room, LocalDate night) {
        BigDecimal rate = room.nightlyRate();
        return Money.round(isHighSeason(night) ? rate.multiply(HIGH_SEASON_FACTOR) : rate);
    }

    /** 15/12 até o fim de fevereiro, e julho inteiro. */
    public boolean isHighSeason(LocalDate date) {
        Month month = date.getMonth();
        return month == Month.JANUARY || month == Month.FEBRUARY || month == Month.JULY
                || (month == Month.DECEMBER && date.getDayOfMonth() >= 15);
    }
}
