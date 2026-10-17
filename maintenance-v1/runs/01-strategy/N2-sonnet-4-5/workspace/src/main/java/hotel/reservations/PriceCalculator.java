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

    private final RatePlanStrategies strategies;

    public PriceCalculator() {
        this.strategies = new RatePlanStrategies();
    }

    public void validate(NewReservation request, Room room) {
        RatePlanStrategy strategy = strategies.get(request.ratePlan());
        strategy.validate(request, room);
    }

    public Price calculate(Room room, LocalDate checkIn, LocalDate checkOut, NewReservation request) {
        RatePlanStrategy strategy = strategies.get(request.ratePlan());
        return strategy.calculate(room, checkIn, checkOut, request);
    }

    public BigDecimal refundAmount(RatePlan ratePlan, BigDecimal amountPaid) {
        RatePlanStrategy strategy = strategies.get(ratePlan);
        return strategy.refundAmount(amountPaid);
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
