package com.marealta;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;

public final class Seasons {
    private Seasons() {}

    /** Alta temporada: 15/12 até o fim de fevereiro, e julho inteiro. */
    public static boolean isHigh(LocalDate night) {
        Month m = night.getMonth();
        return m == Month.JANUARY || m == Month.FEBRUARY || m == Month.JULY
                || (m == Month.DECEMBER && night.getDayOfMonth() >= 15);
    }

    public static BigDecimal nightPrice(Room room, LocalDate night) {
        return isHigh(night) ? Money.percent(room.nightlyRate(), "1.20") : room.nightlyRate();
    }

    /** Soma das noites [checkIn, checkOut), cada uma arredondada a centavos. */
    public static BigDecimal sumNights(Room room, LocalDate checkIn, LocalDate checkOut) {
        BigDecimal sum = Money.ZERO;
        for (LocalDate d = checkIn; d.isBefore(checkOut); d = d.plusDays(1)) {
            sum = sum.add(nightPrice(room, d));
        }
        return Money.round(sum);
    }
}
