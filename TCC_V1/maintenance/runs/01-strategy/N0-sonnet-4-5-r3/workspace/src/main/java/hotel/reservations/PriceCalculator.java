package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Map;

/**
 * The price of a stay: every night costs the room's nightly rate, plus the high-season surcharge on
 * high-season nights; the rate plan may then give a discount on the sum. Money is rounded to cents
 * (half-even) at every step.
 */
public final class PriceCalculator {

    private static final Map<RatePlan, RatePlanStrategy> STRATEGIES = Map.of(
            RatePlan.STANDARD, new StandardRatePlan(),
            RatePlan.NON_REFUNDABLE, new NonRefundableRatePlan(),
            RatePlan.CORPORATE, new CorporateRatePlan(),
            RatePlan.WEEKEND_PACKAGE, new WeekendPackageRatePlan(),
            RatePlan.LONG_STAY, new LongStayRatePlan());

    public Price calculate(
            Room room, LocalDate checkIn, LocalDate checkOut, RatePlan ratePlan, String companyCode) {
        RatePlanStrategy strategy = STRATEGIES.get(ratePlan);
        strategy.validate(room, checkIn, checkOut, companyCode);
        return strategy.calculatePrice(room, checkIn, checkOut, companyCode);
    }

    public BigDecimal calculateRefund(RatePlan ratePlan, BigDecimal amountPaid) {
        RatePlanStrategy strategy = STRATEGIES.get(ratePlan);
        return strategy.calculateRefund(amountPaid);
    }

    static BigDecimal cents(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }
}
