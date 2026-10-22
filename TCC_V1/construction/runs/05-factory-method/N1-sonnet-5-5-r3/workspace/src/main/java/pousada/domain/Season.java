package pousada.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;

/** Alta temporada: 15/12 até fim de fevereiro, e julho inteiro. */
public final class Season {
    private static final BigDecimal HIGH_SEASON_MULTIPLIER = new BigDecimal("1.20");

    private Season() {
    }

    public static boolean isHigh(LocalDate night) {
        Month month = night.getMonth();
        return month == Month.JULY
                || month == Month.JANUARY
                || month == Month.FEBRUARY
                || (month == Month.DECEMBER && night.getDayOfMonth() >= 15);
    }

    public static BigDecimal priceOf(Room room, LocalDate night) {
        BigDecimal rate = room.nightlyRate();
        return isHigh(night) ? Money.round(rate.multiply(HIGH_SEASON_MULTIPLIER)) : rate;
    }
}
