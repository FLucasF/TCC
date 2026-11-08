package br.com.marealta.pousada.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.time.temporal.ChronoUnit;

public record Pricing(int nights, BigDecimal subtotal, BigDecimal discount, BigDecimal total) {
    private static final BigDecimal PEAK_MULTIPLIER = new BigDecimal("1.20");
    private static final BigDecimal NON_REFUNDABLE_DISCOUNT = new BigDecimal("0.15");

    public static Pricing quote(Room room, LocalDate checkIn, LocalDate checkOut, RatePlan plan) {
        BigDecimal subtotal = Money.ZERO;
        for (LocalDate night = checkIn; night.isBefore(checkOut); night = night.plusDays(1)) {
            subtotal = subtotal.add(nightPrice(room, night));
        }
        BigDecimal discount = plan == RatePlan.NON_REFUNDABLE
                ? Money.round(subtotal.multiply(NON_REFUNDABLE_DISCOUNT))
                : Money.ZERO;
        int nights = (int) ChronoUnit.DAYS.between(checkIn, checkOut);
        return new Pricing(nights, subtotal, discount, subtotal.subtract(discount));
    }

    static BigDecimal nightPrice(Room room, LocalDate night) {
        BigDecimal rate = Money.round(room.nightlyRate());
        return isHighSeason(night) ? Money.round(rate.multiply(PEAK_MULTIPLIER)) : rate;
    }

    /** Alta temporada: de 15/12 ao fim de fevereiro, e julho inteiro. */
    static boolean isHighSeason(LocalDate night) {
        Month month = night.getMonth();
        return month == Month.JANUARY || month == Month.FEBRUARY || month == Month.JULY
                || (month == Month.DECEMBER && night.getDayOfMonth() >= 15);
    }
}
