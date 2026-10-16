package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Month;
import java.time.temporal.ChronoUnit;

/**
 * The shared pricing engine: every night costs the room's nightly rate, plus the high-season surcharge
 * on high-season nights. How the sum of the nights turns into a price — any discount or flat price — is
 * the {@link RatePlanPolicy}'s to decide. Money is rounded to cents (half-even) at every step.
 */
public final class PriceCalculator {

    static final BigDecimal HIGH_SEASON_SURCHARGE = new BigDecimal("0.20");

    public Price calculate(Room room, LocalDate checkIn, LocalDate checkOut, RatePlan ratePlan) {
        return ratePlan.policy().price(room, checkIn, checkOut, null, this);
    }

    /** The nights of the stay, each at its nightly rate plus any high-season surcharge, summed. */
    public BigDecimal nightsSubtotal(Room room, LocalDate checkIn, LocalDate checkOut) {
        BigDecimal subtotal = cents(BigDecimal.ZERO);
        for (LocalDate night = checkIn; night.isBefore(checkOut); night = night.plusDays(1)) {
            subtotal = subtotal.add(nightPrice(room, night));
        }
        return subtotal;
    }

    BigDecimal nightPrice(Room room, LocalDate night) {
        BigDecimal nightlyRate = room.nightlyRate();
        if (isHighSeason(night)) {
            return cents(nightlyRate.multiply(BigDecimal.ONE.add(HIGH_SEASON_SURCHARGE)));
        }
        return cents(nightlyRate);
    }

    static int nights(LocalDate checkIn, LocalDate checkOut) {
        return (int) ChronoUnit.DAYS.between(checkIn, checkOut);
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
