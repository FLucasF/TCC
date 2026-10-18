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

    static final BigDecimal HIGH_SEASON_SURCHARGE = new BigDecimal("0.25");
    static final BigDecimal NON_REFUNDABLE_DISCOUNT = new BigDecimal("0.15");

    public Price calculate(Room room, LocalDate checkIn, LocalDate checkOut, RatePlan ratePlan) {
        int nights = 0;
        BigDecimal subtotal = cents(BigDecimal.ZERO);
        for (LocalDate night = checkIn; night.isBefore(checkOut); night = night.plusDays(1)) {
            subtotal = subtotal.add(nightPrice(room, night));
            nights++;
        }
        BigDecimal discount = switch (ratePlan) {
            case STANDARD -> cents(BigDecimal.ZERO);
            case NON_REFUNDABLE -> cents(subtotal.multiply(NON_REFUNDABLE_DISCOUNT));
        };
        return new Price(nights, subtotal, discount, subtotal.subtract(discount));
    }

    BigDecimal nightPrice(Room room, LocalDate night) {
        BigDecimal nightlyRate = room.nightlyRate();
        if (isHighSeason(night)) {
            return cents(nightlyRate.multiply(BigDecimal.ONE.add(HIGH_SEASON_SURCHARGE)));
        }
        return cents(nightlyRate);
    }

    /** High season: from December 15 to the end of February, the whole of July, and Easter week. */
    static boolean isHighSeason(LocalDate date) {
        Month month = date.getMonth();
        if (month == Month.JANUARY || month == Month.FEBRUARY || month == Month.JULY) {
            return true;
        }
        if (month == Month.DECEMBER && date.getDayOfMonth() >= 15) {
            return true;
        }
        // Easter week: March 21-28 (2027) and April 9-16 (2028)
        int year = date.getYear();
        int day = date.getDayOfMonth();
        if (year == 2027 && month == Month.MARCH && day >= 21 && day <= 28) {
            return true;
        }
        if (year == 2028 && month == Month.APRIL && day >= 9 && day <= 16) {
            return true;
        }
        return false;
    }

    static BigDecimal cents(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }
}
