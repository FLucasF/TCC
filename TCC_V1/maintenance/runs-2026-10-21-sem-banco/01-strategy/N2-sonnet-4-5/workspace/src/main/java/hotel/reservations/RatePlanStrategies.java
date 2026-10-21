package hotel.reservations;

import java.util.Map;

/** Maps each rate plan to its strategy. */
final class RatePlanStrategies {

    private static final Map<RatePlan, RatePlanStrategy> STRATEGIES = Map.of(
            RatePlan.STANDARD, new StandardRatePlan(),
            RatePlan.NON_REFUNDABLE, new NonRefundableRatePlan(),
            RatePlan.CORPORATE, new CorporateRatePlan(),
            RatePlan.WEEKEND_PACKAGE, new WeekendPackageRatePlan(),
            RatePlan.LONG_STAY, new LongStayRatePlan());

    private RatePlanStrategies() {
    }

    static RatePlanStrategy strategyFor(RatePlan plan) {
        return STRATEGIES.get(plan);
    }
}
