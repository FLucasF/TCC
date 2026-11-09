package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.function.Predicate;

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

    /**
     * High season: from December 15 to the end of February, the whole of July, and Holy Week (Palm
     * Sunday to Easter Sunday, both included) in the years listed. A new season, or a new year of
     * Easter, is one more entry here.
     */
    private static final List<Predicate<LocalDate>> HIGH_SEASONS = List.of(
            date -> date.getMonth() == Month.JANUARY || date.getMonth() == Month.FEBRUARY,
            date -> date.getMonth() == Month.DECEMBER && date.getDayOfMonth() >= 15,
            date -> date.getMonth() == Month.JULY,
            between(LocalDate.of(2027, 3, 21), LocalDate.of(2027, 3, 28)),
            between(LocalDate.of(2028, 4, 9), LocalDate.of(2028, 4, 16)));

    static boolean isHighSeason(LocalDate date) {
        return HIGH_SEASONS.stream().anyMatch(season -> season.test(date));
    }

    private static Predicate<LocalDate> between(LocalDate first, LocalDate last) {
        return date -> !date.isBefore(first) && !date.isAfter(last);
    }

    static BigDecimal cents(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }
}
