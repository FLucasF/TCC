package hotel.reservations;

import hotel.reservations.plans.PricingSupport;
import hotel.rooms.Room;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Month;
import java.time.temporal.ChronoUnit;

/**
 * Turns a stay into money. The nightly primitives (every night costs the room's nightly rate, plus
 * the high-season surcharge on high-season nights, rounded half-even to cents) live here; how the
 * subtotal becomes a total belongs to the request's rate plan.
 */
public final class PriceCalculator implements PricingSupport {

    static final BigDecimal HIGH_SEASON_SURCHARGE = new BigDecimal("0.20");

    public Price calculate(Room room, NewReservation request) {
        return request.ratePlan().policy().price(room, request, this);
    }

    @Override
    public int nights(LocalDate checkIn, LocalDate checkOut) {
        return (int) ChronoUnit.DAYS.between(checkIn, checkOut);
    }

    @Override
    public BigDecimal subtotal(Room room, LocalDate checkIn, LocalDate checkOut) {
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

    /** High season: from December 15 to the end of February, and the whole of July. */
    public static boolean isHighSeason(LocalDate date) {
        Month month = date.getMonth();
        return month == Month.JANUARY
                || month == Month.FEBRUARY
                || month == Month.JULY
                || (month == Month.DECEMBER && date.getDayOfMonth() >= 15);
    }

    @Override
    public BigDecimal cents(BigDecimal value) {
        return roundToCents(value);
    }

    public static BigDecimal roundToCents(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }
}
