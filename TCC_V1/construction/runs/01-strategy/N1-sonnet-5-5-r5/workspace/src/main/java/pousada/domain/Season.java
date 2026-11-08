package pousada.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;

/** Alta temporada: de 15 de dezembro ao fim de fevereiro, e julho inteiro (+20% na noite). */
public final class Season {
    private static final int HIGH_SEASON_PERCENT = 120;

    private Season() {
    }

    public static boolean isHigh(LocalDate night) {
        Month month = night.getMonth();
        return month == Month.JANUARY
                || month == Month.FEBRUARY
                || month == Month.JULY
                || (month == Month.DECEMBER && night.getDayOfMonth() >= 15);
    }

    public static BigDecimal nightPrice(BigDecimal nightlyRate, LocalDate night) {
        return isHigh(night) ? Money.percent(nightlyRate, HIGH_SEASON_PERCENT) : nightlyRate;
    }
}
