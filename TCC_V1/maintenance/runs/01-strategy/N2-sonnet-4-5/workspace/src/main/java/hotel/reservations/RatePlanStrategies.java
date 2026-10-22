package hotel.reservations;

import java.util.Map;

/** Maps each rate plan to its strategy. */
final class RatePlanStrategies {

    private final Map<RatePlan, RatePlanStrategy> strategies;

    RatePlanStrategies(PriceCalculator priceCalculator) {
        this.strategies = Map.of(
                RatePlan.STANDARD, new StandardRatePlan(priceCalculator),
                RatePlan.NON_REFUNDABLE, new NonRefundableRatePlan(priceCalculator),
                RatePlan.CORPORATE, new CorporateRatePlan(priceCalculator),
                RatePlan.WEEKEND_PACKAGE, new WeekendPackageRatePlan(),
                RatePlan.LONG_STAY, new LongStayRatePlan(priceCalculator));
    }

    RatePlanStrategy get(RatePlan ratePlan) {
        return strategies.get(ratePlan);
    }
}
