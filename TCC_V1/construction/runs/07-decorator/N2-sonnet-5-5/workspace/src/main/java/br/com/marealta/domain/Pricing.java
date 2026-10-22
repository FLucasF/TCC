package br.com.marealta.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.time.temporal.ChronoUnit;

public final class Pricing {
    private static final BigDecimal HIGH_SEASON_FACTOR = new BigDecimal("1.20");

    public record Quote(int nights, BigDecimal subtotal, BigDecimal discount, BigDecimal total) {}

    private Pricing() {}

    /** Alta temporada: de 15 de dezembro ao fim de fevereiro e todo o mês de julho. */
    public static boolean isHighSeason(LocalDate night) {
        Month month = night.getMonth();
        return month == Month.JULY
                || month == Month.JANUARY
                || month == Month.FEBRUARY
                || (month == Month.DECEMBER && night.getDayOfMonth() >= 15);
    }

    public static BigDecimal nightPrice(Room room, LocalDate night) {
        BigDecimal rate = room.nightlyRate();
        return isHighSeason(night) ? Money.round(rate.multiply(HIGH_SEASON_FACTOR)) : Money.round(rate);
    }

    public static Quote quote(Room room, LocalDate checkIn, LocalDate checkOut, RatePlan plan) {
        int nights = (int) ChronoUnit.DAYS.between(checkIn, checkOut);
        BigDecimal subtotal = Money.ZERO;
        for (LocalDate night = checkIn; night.isBefore(checkOut); night = night.plusDays(1)) {
            subtotal = Money.round(subtotal.add(nightPrice(room, night)));
        }
        BigDecimal discount = plan.discountOn(subtotal);
        BigDecimal total = Money.round(subtotal.subtract(discount));
        return new Quote(nights, subtotal, discount, total);
    }
}
