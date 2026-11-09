package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;

/**
 * The price of a stay: every night costs the room's nightly rate, plus the high-season surcharge on
 * high-season nights; the rate plan may then give a discount on the sum. Money is rounded to cents
 * (half-even) at every step.
 */
public final class PriceCalculator {

    static final BigDecimal HIGH_SEASON_SURCHARGE = new BigDecimal("0.25");
    static final BigDecimal NON_REFUNDABLE_DISCOUNT = new BigDecimal("0.15");

    /** Holy Week: Palm Sunday through Easter Sunday, both nights included. */
    private record Period(LocalDate first, LocalDate last) {
        boolean contains(LocalDate date) {
            return !date.isBefore(first) && !date.isAfter(last);
        }
    }

    /** Only these years are known; later years must be added. */
    private static final List<Period> HOLY_WEEKS = List.of(
            new Period(LocalDate.of(2027, 3, 21), LocalDate.of(2027, 3, 28)),
            new Period(LocalDate.of(2028, 4, 9), LocalDate.of(2028, 4, 16)));

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

    /** High season: from December 15 to the end of February, the whole of July, and Holy Week. */
    static boolean isHighSeason(LocalDate date) {
        Month month = date.getMonth();
        return HOLY_WEEKS.stream().anyMatch(week -> week.contains(date))
                || month == Month.JANUARY
                || month == Month.FEBRUARY
                || month == Month.JULY
                || (month == Month.DECEMBER && date.getDayOfMonth() >= 15);
    }

    static BigDecimal cents(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }
}
