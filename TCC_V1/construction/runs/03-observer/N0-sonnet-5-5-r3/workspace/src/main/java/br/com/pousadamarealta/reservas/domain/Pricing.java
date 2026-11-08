package br.com.pousadamarealta.reservas.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;

/** Cálculo do valor de uma estadia. */
public final class Pricing {
    private static final BigDecimal HIGH_SEASON_FACTOR = new BigDecimal("1.20");

    private Pricing() {
    }

    public record Quote(int nights, BigDecimal subtotal, BigDecimal discount, BigDecimal total) {
    }

    /** Alta temporada: de 15/12 ao fim de fevereiro, e julho inteiro. */
    public static boolean isHighSeason(LocalDate night) {
        Month month = night.getMonth();
        return month == Month.JANUARY
                || month == Month.FEBRUARY
                || month == Month.JULY
                || (month == Month.DECEMBER && night.getDayOfMonth() >= 15);
    }

    public static BigDecimal nightPrice(Room room, LocalDate night) {
        BigDecimal rate = room.nightlyRate();
        return Money.scale(isHighSeason(night) ? rate.multiply(HIGH_SEASON_FACTOR) : rate);
    }

    /** As noites vão de checkIn (inclusive) a checkOut (exclusive). */
    public static Quote quote(Room room, LocalDate checkIn, LocalDate checkOut, RatePlan plan) {
        BigDecimal subtotal = Money.ZERO;
        int nights = 0;
        for (LocalDate night = checkIn; night.isBefore(checkOut); night = night.plusDays(1)) {
            subtotal = subtotal.add(nightPrice(room, night));
            nights++;
        }
        BigDecimal discount = Money.scale(subtotal.multiply(plan.discountRate()));
        BigDecimal total = Money.scale(subtotal.subtract(discount));
        return new Quote(nights, subtotal, discount, total);
    }
}
