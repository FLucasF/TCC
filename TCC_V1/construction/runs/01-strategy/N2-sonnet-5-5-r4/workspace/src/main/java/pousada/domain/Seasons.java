package pousada.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Calendário de temporada e preço das noites de um quarto. */
public final class Seasons {
    private static final BigDecimal HIGH_SEASON_FACTOR = new BigDecimal("1.20");

    private Seasons() {}

    /** Alta temporada: 15/12 até o fim de fevereiro, e julho inteiro. */
    public static boolean isHighSeason(LocalDate night) {
        int month = night.getMonthValue();
        return month == 1 || month == 2 || month == 7 || (month == 12 && night.getDayOfMonth() >= 15);
    }

    public static BigDecimal nightPrice(Room room, LocalDate night) {
        BigDecimal rate = room.nightlyRate();
        return Money.round(isHighSeason(night) ? rate.multiply(HIGH_SEASON_FACTOR) : rate);
    }

    /** Soma das noites [checkIn, checkOut), cada uma arredondada. */
    public static BigDecimal sumOfNights(Room room, LocalDate checkIn, LocalDate checkOut) {
        BigDecimal sum = Money.ZERO;
        for (LocalDate night = checkIn; night.isBefore(checkOut); night = night.plusDays(1)) {
            sum = sum.add(nightPrice(room, night));
        }
        return Money.round(sum);
    }
}
