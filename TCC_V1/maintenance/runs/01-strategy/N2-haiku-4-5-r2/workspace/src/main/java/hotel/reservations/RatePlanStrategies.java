package hotel.reservations;

import java.util.Map;

/** The one place that knows which strategy implements each rate plan. */
final class RatePlanStrategies {

    private final Map<RatePlan, RatePlanStrategy> strategies;

    RatePlanStrategies(PriceCalculator priceCalculator) {
        this.strategies = Map.of(
                RatePlan.STANDARD, new StandardPlanStrategy(priceCalculator),
                RatePlan.NON_REFUNDABLE, new NonRefundablePlanStrategy(priceCalculator),
                RatePlan.CORPORATE, new CorporatePlanStrategy(priceCalculator),
                RatePlan.WEEKEND_PACKAGE, new WeekendPackagePlanStrategy(),
                RatePlan.LONG_STAY, new LongStayPlanStrategy(priceCalculator));
    }

    RatePlanStrategy forPlan(RatePlan plan) {
        return strategies.get(plan);
    }
}
