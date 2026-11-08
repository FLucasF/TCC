package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;

import static hotel.reservations.PriceCalculator.cents;

/** Base implementation with common pricing logic. */
abstract class BaseRatePlanStrategy implements RatePlanStrategy {

    static final BigDecimal HIGH_SEASON_SURCHARGE = new BigDecimal("0.20");

    @Override
    public void validate(Room room, LocalDate checkIn, LocalDate checkOut, String companyCode) {
        // Most plans have no extra validation
    }

    /**
     * Calculates the sum of nightly rates (with high-season surcharge) across all nights.
     */
    protected BigDecimal calculateSubtotal(Room room, LocalDate checkIn, LocalDate checkOut) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (LocalDate night = checkIn; night.isBefore(checkOut); night = night.plusDays(1)) {
            subtotal = subtotal.add(nightPrice(room, night));
        }
        return cents(subtotal);
    }

    protected BigDecimal nightPrice(Room room, LocalDate night) {
        BigDecimal nightlyRate = room.nightlyRate();
        if (isHighSeason(night)) {
            return cents(nightlyRate.multiply(BigDecimal.ONE.add(HIGH_SEASON_SURCHARGE)));
        }
        return cents(nightlyRate);
    }

    protected int countNights(LocalDate checkIn, LocalDate checkOut) {
        int nights = 0;
        for (LocalDate night = checkIn; night.isBefore(checkOut); night = night.plusDays(1)) {
            nights++;
        }
        return nights;
    }

    /** High season: from December 15 to the end of February, and the whole of July. */
    protected static boolean isHighSeason(LocalDate date) {
        Month month = date.getMonth();
        return month == Month.JANUARY
                || month == Month.FEBRUARY
                || month == Month.JULY
                || (month == Month.DECEMBER && date.getDayOfMonth() >= 15);
    }
}
