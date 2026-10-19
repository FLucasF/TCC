package pousada;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;

import static pousada.Money.round;

public final class Pricing {
    private static final BigDecimal HIGH_SEASON_FACTOR = new BigDecimal("1.20");

    private Pricing() {
    }

    public static boolean isHighSeason(LocalDate night) {
        Month month = night.getMonth();
        return month == Month.JANUARY || month == Month.FEBRUARY || month == Month.JULY
                || (month == Month.DECEMBER && night.getDayOfMonth() >= 15);
    }

    public static BigDecimal nightPrice(RoomType type, LocalDate night) {
        BigDecimal rate = type.nightlyRate;
        return round(isHighSeason(night) ? rate.multiply(HIGH_SEASON_FACTOR) : rate);
    }

    /** Soma das noites de [checkIn, checkOut). */
    public static BigDecimal subtotal(RoomType type, LocalDate checkIn, LocalDate checkOut) {
        BigDecimal sum = round(BigDecimal.ZERO);
        for (LocalDate night = checkIn; night.isBefore(checkOut); night = night.plusDays(1)) {
            sum = round(sum.add(nightPrice(type, night)));
        }
        return sum;
    }
}
