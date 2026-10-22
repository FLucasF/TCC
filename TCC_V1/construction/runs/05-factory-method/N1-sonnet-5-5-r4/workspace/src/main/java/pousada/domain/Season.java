package pousada.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Alta temporada: de 15 de dezembro ao fim de fevereiro, e julho inteiro. */
public final class Season {
    private static final BigDecimal HIGH_SEASON_MULTIPLIER = new BigDecimal("1.20");

    private Season() {
    }

    public static boolean isHigh(LocalDate night) {
        int month = night.getMonthValue();
        return month == 1 || month == 2 || month == 7
                || (month == 12 && night.getDayOfMonth() >= 15);
    }

    public static BigDecimal priceOfNight(BigDecimal nightlyRate, LocalDate night) {
        return isHigh(night) ? Money.round(nightlyRate.multiply(HIGH_SEASON_MULTIPLIER)) : nightlyRate;
    }
}
