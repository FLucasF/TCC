package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Month;
import java.util.Map;

/**
 * The price of a stay: every night costs the room's nightly rate, plus the high-season surcharge on
 * high-season nights; the rate plan may then give a discount on the sum. Money is rounded to cents
 * (half-even) at every step.
 */
public final class PriceCalculator {

    static final BigDecimal HIGH_SEASON_SURCHARGE = new BigDecimal("0.20");
    static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_EVEN;

    private final Map<RatePlan, RatePlanStrategy> strategies;

    public PriceCalculator() {
        this.strategies = Map.of(
                RatePlan.STANDARD, new StandardRatePlanStrategy(this),
                RatePlan.NON_REFUNDABLE, new NonRefundableRatePlanStrategy(this),
                RatePlan.CORPORATE, new CorporateRatePlanStrategy(this),
                RatePlan.WEEKEND_PACKAGE, new WeekendPackageRatePlanStrategy(),
                RatePlan.LONG_STAY, new LongStayRatePlanStrategy(this));
    }

    public Price calculate(Room room, LocalDate checkIn, LocalDate checkOut, RatePlan ratePlan, String companyCode) {
        RatePlanStrategy strategy = strategies.get(ratePlan);
        if (ratePlan == RatePlan.CORPORATE) {
            return ((CorporateRatePlanStrategy) strategy).calculatePrice(room, checkIn, checkOut, companyCode);
        }
        return strategy.calculatePrice(room, checkIn, checkOut);
    }

    RatePlanStrategy strategyFor(RatePlan ratePlan) {
        return strategies.get(ratePlan);
    }

    /** Calculates price based on nightly rates with optional discount. */
    Price calculateByNightlyRate(Room room, LocalDate checkIn, LocalDate checkOut, BigDecimal discountRate) {
        int nights = 0;
        BigDecimal subtotal = cents(BigDecimal.ZERO);
        for (LocalDate night = checkIn; night.isBefore(checkOut); night = night.plusDays(1)) {
            subtotal = subtotal.add(nightPrice(room, night));
            nights++;
        }
        BigDecimal discount = cents(subtotal.multiply(discountRate));
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
        return value.setScale(2, ROUNDING_MODE);
    }
}
