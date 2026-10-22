package br.com.marealta.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;

public final class Pricing {
    private static final BigDecimal HIGH_SEASON = new BigDecimal("1.20");
    private static final BigDecimal NON_REFUNDABLE_DISCOUNT = new BigDecimal("0.15");

    private Pricing() {}

    public record Quote(int nights, BigDecimal subtotal, BigDecimal discount, BigDecimal total) {}

    /** Alta temporada: de 15/12 ao fim de fevereiro e todo o mês de julho. */
    public static boolean isHighSeason(LocalDate night) {
        Month m = night.getMonth();
        return m == Month.JANUARY || m == Month.FEBRUARY || m == Month.JULY
                || (m == Month.DECEMBER && night.getDayOfMonth() >= 15);
    }

    public static BigDecimal nightPrice(Room room, LocalDate night) {
        BigDecimal rate = room.nightlyRate();
        return isHighSeason(night) ? Money.round(rate.multiply(HIGH_SEASON)) : Money.round(rate);
    }

    public static Quote quote(Room room, LocalDate checkIn, LocalDate checkOut, RatePlan plan) {
        BigDecimal subtotal = Money.ZERO;
        int nights = 0;
        for (LocalDate d = checkIn; d.isBefore(checkOut); d = d.plusDays(1)) {
            subtotal = subtotal.add(nightPrice(room, d));
            nights++;
        }
        subtotal = Money.round(subtotal);
        BigDecimal discount = plan == RatePlan.NON_REFUNDABLE
                ? Money.round(subtotal.multiply(NON_REFUNDABLE_DISCOUNT)) : Money.ZERO;
        return new Quote(nights, subtotal, discount, Money.round(subtotal.subtract(discount)));
    }
}
