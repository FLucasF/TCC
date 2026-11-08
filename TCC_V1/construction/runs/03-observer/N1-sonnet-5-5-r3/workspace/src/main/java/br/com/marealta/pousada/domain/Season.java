package br.com.marealta.pousada.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;

/** Alta temporada: de 15/12 ao fim de fevereiro e todo julho. */
public final class Season {

    private static final BigDecimal HIGH_MULTIPLIER = new BigDecimal("1.20");

    private Season() {
    }

    public static boolean isHigh(LocalDate night) {
        Month month = night.getMonth();
        return month == Month.JANUARY
                || month == Month.FEBRUARY
                || month == Month.JULY
                || (month == Month.DECEMBER && night.getDayOfMonth() >= 15);
    }

    public static BigDecimal nightPrice(Room room, LocalDate night) {
        BigDecimal rate = room.nightlyRate();
        return Money.round(isHigh(night) ? rate.multiply(HIGH_MULTIPLIER) : rate);
    }
}
