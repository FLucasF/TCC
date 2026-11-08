package br.com.marealta.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;

/** High season: from December 15th to the end of February, and the whole month of July. */
public final class Season {
    public static final BigDecimal HIGH_SEASON_MULTIPLIER = new BigDecimal("1.20");

    private Season() {
    }

    public static boolean isHigh(LocalDate night) {
        Month month = night.getMonth();
        return month == Month.JANUARY
                || month == Month.FEBRUARY
                || month == Month.JULY
                || (month == Month.DECEMBER && night.getDayOfMonth() >= 15);
    }
}
