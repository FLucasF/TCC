package hotel.reservations;

import hotel.reservations.rateplans.RatePlanPolicies;
import hotel.rooms.Room;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Month;

/**
 * Nightly pricing engine and entry point for a stay's full price. Every night costs the room's
 * nightly rate, plus the high-season surcharge on high-season nights; the plan's policy then shapes
 * the subtotal into the final {@link Price}. Money is rounded to cents (half-even) at every step.
 */
public final class PriceCalculator {

    static final BigDecimal HIGH_SEASON_SURCHARGE = new BigDecimal("0.20");

    private final RatePlanPolicies policies;

    public PriceCalculator() {
        this(new RatePlanPolicies());
    }

    public PriceCalculator(RatePlanPolicies policies) {
        this.policies = policies;
    }

    public Price calculate(Room room, LocalDate checkIn, LocalDate checkOut, RatePlan ratePlan) {
        return calculate(room, new NewReservation(room.number(), null, checkIn, checkOut, 1, ratePlan));
    }

    public Price calculate(Room room, NewReservation request) {
        return policies.of(request.ratePlan()).price(request, room, this);
    }

    /** Number of nights in [checkIn, checkOut). */
    public int nights(LocalDate checkIn, LocalDate checkOut) {
        return (int) checkIn.until(checkOut, java.time.temporal.ChronoUnit.DAYS);
    }

    /** Sum of nightly prices across the stay, rounded to cents at each night. */
    public BigDecimal subtotal(Room room, LocalDate checkIn, LocalDate checkOut) {
        BigDecimal subtotal = cents(BigDecimal.ZERO);
        for (LocalDate night = checkIn; night.isBefore(checkOut); night = night.plusDays(1)) {
            subtotal = subtotal.add(nightPrice(room, night));
        }
        return subtotal;
    }

    /** The subtotal, with a flat discount fraction applied to it (half-even to cents). */
    public Price nightlyPlanPrice(Room room, LocalDate checkIn, LocalDate checkOut, BigDecimal discountFraction) {
        int nights = nights(checkIn, checkOut);
        BigDecimal subtotal = subtotal(room, checkIn, checkOut);
        BigDecimal discount = cents(subtotal.multiply(discountFraction));
        return new Price(nights, subtotal, discount, subtotal.subtract(discount));
    }

    public BigDecimal nightPrice(Room room, LocalDate night) {
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

    public static BigDecimal cents(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }
}
