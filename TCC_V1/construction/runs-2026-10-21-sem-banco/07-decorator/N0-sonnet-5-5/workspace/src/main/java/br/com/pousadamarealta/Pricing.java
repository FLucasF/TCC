package br.com.pousadamarealta;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;

public final class Pricing {
    private static final BigDecimal HIGH_SEASON_FACTOR = new BigDecimal("1.20");

    private Pricing() {}

    /** High season: Dec 15 to end of February, and all of July. */
    public static boolean highSeason(LocalDate night) {
        Month m = night.getMonth();
        return m == Month.JANUARY || m == Month.FEBRUARY || m == Month.JULY
                || (m == Month.DECEMBER && night.getDayOfMonth() >= 15);
    }

    public static BigDecimal nightPrice(Room room, LocalDate night) {
        BigDecimal base = room.nightlyRate();
        return highSeason(night) ? Money.round(base.multiply(HIGH_SEASON_FACTOR)) : Money.round(base);
    }

    public static BigDecimal subtotal(Room room, LocalDate checkIn, LocalDate checkOut) {
        BigDecimal sum = Money.ZERO;
        for (LocalDate d = checkIn; d.isBefore(checkOut); d = d.plusDays(1)) {
            sum = Money.round(sum.add(nightPrice(room, d)));
        }
        return sum;
    }
}
