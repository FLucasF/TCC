package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Month;

/**
 * The price of a stay: the rate plan prices it, usually from the nights, where every night costs the
 * room's nightly rate plus the high-season surcharge on high-season nights. Money is rounded to cents
 * (half-even) at every step.
 */
public final class PriceCalculator {

    static final BigDecimal HIGH_SEASON_SURCHARGE = new BigDecimal("0.20");

    public Price calculate(Stay stay, RatePlan ratePlan) {
        return ratePlan.price(stay);
    }

    /** The nights of the stay, each at the room's rate with the high-season surcharge. */
    static BigDecimal nightsSubtotal(Stay stay) {
        BigDecimal subtotal = cents(BigDecimal.ZERO);
        for (LocalDate night = stay.checkIn(); night.isBefore(stay.checkOut()); night = night.plusDays(1)) {
            subtotal = subtotal.add(nightPrice(stay.room(), night));
        }
        return subtotal;
    }

    static BigDecimal nightPrice(Room room, LocalDate night) {
        BigDecimal nightlyRate = room.nightlyRate();
        if (isHighSeason(night)) {
            return cents(nightlyRate.multiply(BigDecimal.ONE.add(HIGH_SEASON_SURCHARGE)));
        }
        return cents(nightlyRate);
    }

    /** High season: from December 15 to the end of February, and the whole of July. */
    static boolean isHighSeason(LocalDate date) {
        Month month = date.getMonth();
        return month == Month.JANUARY
                || month == Month.FEBRUARY
                || month == Month.JULY
                || (month == Month.DECEMBER && date.getDayOfMonth() >= 15);
    }

    static BigDecimal cents(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }
}
