package br.com.marealta;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;

public final class Season {
    private static final BigDecimal HIGH_SEASON_FACTOR = new BigDecimal("1.20");

    private Season() {
    }

    /** Alta temporada: de 15/12 ao fim de fevereiro, e julho inteiro. */
    public static boolean isHigh(LocalDate night) {
        Month month = night.getMonth();
        return month == Month.JANUARY || month == Month.FEBRUARY || month == Month.JULY
                || (month == Month.DECEMBER && night.getDayOfMonth() >= 15);
    }

    public static BigDecimal nightPrice(Room room, LocalDate night) {
        BigDecimal rate = room.type().nightlyRate();
        return isHigh(night) ? Money.fraction(rate, HIGH_SEASON_FACTOR) : rate;
    }
}
