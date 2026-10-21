package br.com.pousadamarealta.reservas.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;

public final class Pricing {

    private static final BigDecimal HIGH_SEASON_FACTOR = new BigDecimal("1.20");

    private Pricing() {
    }

    public static Quote quote(Room room, LocalDate checkIn, LocalDate checkOut, RatePlan plan) {
        BigDecimal subtotal = Money.ZERO;
        int nights = 0;
        for (LocalDate night = checkIn; night.isBefore(checkOut); night = night.plusDays(1)) {
            subtotal = subtotal.add(nightPrice(room, night));
            nights++;
        }
        BigDecimal discount = plan.discount(subtotal);
        return new Quote(nights, subtotal, discount, subtotal.subtract(discount));
    }

    static BigDecimal nightPrice(Room room, LocalDate night) {
        if (isHighSeason(night)) {
            return Money.round(room.nightlyRate().multiply(HIGH_SEASON_FACTOR));
        }
        return room.nightlyRate();
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
