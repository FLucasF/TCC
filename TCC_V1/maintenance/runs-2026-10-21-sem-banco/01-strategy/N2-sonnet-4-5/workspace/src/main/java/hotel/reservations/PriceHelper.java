package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Month;

/** Shared calculations for rate plan strategies. */
public final class PriceHelper {

    static final BigDecimal HIGH_SEASON_SURCHARGE = new BigDecimal("0.20");

    private PriceHelper() {
    }

    /** The price of one night, with the high-season surcharge when applicable. */
    public static BigDecimal nightPrice(Room room, LocalDate night) {
        BigDecimal nightlyRate = room.nightlyRate();
        if (isHighSeason(night)) {
            return cents(nightlyRate.multiply(BigDecimal.ONE.add(HIGH_SEASON_SURCHARGE)));
        }
        return cents(nightlyRate);
    }

    /** High season: from December 15 to the end of February, and the whole of July. */
    public static boolean isHighSeason(LocalDate date) {
        Month month = date.getMonth();
        return month == Month.JANUARY
                || month == Month.FEBRUARY
                || month == Month.JULY
                || (month == Month.DECEMBER && date.getDayOfMonth() >= 15);
    }

    /** Rounds to cents (half-even). */
    public static BigDecimal cents(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }
}
