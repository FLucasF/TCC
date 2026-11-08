package br.com.mare.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;

/** Alta temporada: de 15/12 ao fim de fevereiro e todo o mês de julho. */
public final class Season {
    private static final BigDecimal HIGH_SEASON_FACTOR = new BigDecimal("1.20");

    private Season() {
    }

    public static boolean isHigh(LocalDate night) {
        Month month = night.getMonth();
        return month == Month.JANUARY
                || month == Month.FEBRUARY
                || month == Month.JULY
                || (month == Month.DECEMBER && night.getDayOfMonth() >= 15);
    }

    public static BigDecimal priceOf(BigDecimal nightlyRate, LocalDate night) {
        return isHigh(night) ? Money.round(nightlyRate.multiply(HIGH_SEASON_FACTOR)) : nightlyRate;
    }
}
