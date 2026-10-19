package br.com.marealta;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class Season {
    private static final BigDecimal HIGH_SEASON_MULTIPLIER = new BigDecimal("1.20");

    private Season() {}

    /** Alta temporada: de 15/12 ao fim de fevereiro e todo o mês de julho. */
    public static boolean isHigh(LocalDate night) {
        int month = night.getMonthValue();
        return month == 1 || month == 2 || month == 7
                || (month == 12 && night.getDayOfMonth() >= 15);
    }

    public static BigDecimal priceOfNight(Room room, LocalDate night) {
        BigDecimal rate = room.nightlyRate();
        return isHigh(night) ? Money.round(rate.multiply(HIGH_SEASON_MULTIPLIER)) : rate;
    }
}
