package br.com.marealta.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;

/** Preço das noites de uma estadia, já com temporada e plano de tarifa. */
public final class Pricing {

    private static final BigDecimal HIGH_SEASON_FACTOR = new BigDecimal("1.20");

    private Pricing() {
    }

    public record Quote(int nights, BigDecimal subtotal, BigDecimal discount, BigDecimal total) {
    }

    public static Quote quote(Room room, LocalDate checkIn, LocalDate checkOut, RatePlan plan) {
        BigDecimal subtotal = Money.ZERO;
        int nights = 0;
        for (LocalDate night = checkIn; night.isBefore(checkOut); night = night.plusDays(1)) {
            subtotal = subtotal.add(nightPrice(room, night));
            nights++;
        }
        subtotal = Money.round(subtotal);
        BigDecimal discount = plan.discount(subtotal);
        return new Quote(nights, subtotal, discount, Money.round(subtotal.subtract(discount)));
    }

    public static BigDecimal nightPrice(Room room, LocalDate night) {
        BigDecimal rate = room.nightlyRate();
        return Money.round(isHighSeason(night) ? rate.multiply(HIGH_SEASON_FACTOR) : rate);
    }

    /** De 15 de dezembro ao fim de fevereiro, e julho inteiro. */
    public static boolean isHighSeason(LocalDate day) {
        Month month = day.getMonth();
        return month == Month.JANUARY
                || month == Month.FEBRUARY
                || month == Month.JULY
                || (month == Month.DECEMBER && day.getDayOfMonth() >= 15);
    }
}
