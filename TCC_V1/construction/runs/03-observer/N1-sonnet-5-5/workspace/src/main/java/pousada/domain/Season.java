package pousada.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Alta temporada: de 15/12 ao fim de fevereiro e todo o mês de julho. */
public final class Season {
    private static final BigDecimal HIGH_SEASON_FACTOR = new BigDecimal("1.20");

    private Season() {
    }

    public static boolean isHigh(LocalDate night) {
        int month = night.getMonthValue();
        return month == 1 || month == 2 || month == 7
                || (month == 12 && night.getDayOfMonth() >= 15);
    }

    public static BigDecimal nightPrice(BigDecimal nightlyRate, LocalDate night) {
        return isHigh(night) ? Money.round(nightlyRate.multiply(HIGH_SEASON_FACTOR)) : nightlyRate;
    }
}
