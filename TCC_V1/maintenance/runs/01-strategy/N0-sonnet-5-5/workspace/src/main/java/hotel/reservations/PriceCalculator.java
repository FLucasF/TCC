package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Month;

/**
 * The price of a stay: every night costs the room's nightly rate, plus the high-season surcharge on
 * high-season nights; the rate plan may then give a discount on the sum. Money is rounded to cents
 * (half-even) at every step.
 */
public final class PriceCalculator {

    static final BigDecimal HIGH_SEASON_SURCHARGE = new BigDecimal("0.20");

    /** Prices a stay for a plan that needs nothing besides the room and the dates. */
    public Price calculate(Room room, LocalDate checkIn, LocalDate checkOut, RatePlan ratePlan) {
        return calculate(room, new NewReservation(room.number(), null, checkIn, checkOut, 1, ratePlan, null));
    }

    /** Prices the stay, or throws INVALID_REQUEST when the rate plan cannot be used for it. */
    public Price calculate(Room room, NewReservation request) {
        return request.ratePlan().policy().price(this, room, request);
    }

    /** The nights at their prices, with a percentage of their sum off. */
    Price discounted(Room room, LocalDate checkIn, LocalDate checkOut, BigDecimal percentage) {
        int nights = 0;
        BigDecimal subtotal = cents(BigDecimal.ZERO);
        for (LocalDate night = checkIn; night.isBefore(checkOut); night = night.plusDays(1)) {
            subtotal = subtotal.add(nightPrice(room, night));
            nights++;
        }
        BigDecimal discount = cents(subtotal.multiply(percentage));
        return new Price(nights, subtotal, discount, subtotal.subtract(discount));
    }

    BigDecimal nightPrice(Room room, LocalDate night) {
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
