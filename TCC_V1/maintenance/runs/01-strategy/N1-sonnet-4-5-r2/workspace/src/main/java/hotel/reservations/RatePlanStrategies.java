package hotel.reservations;

import java.util.Map;

final class RatePlanStrategies {

    private static final Map<RatePlan, RatePlanStrategy> STRATEGIES = Map.of(
            RatePlan.STANDARD, new StandardPlan(),
            RatePlan.NON_REFUNDABLE, new NonRefundablePlan(),
            RatePlan.CORPORATE, new CorporatePlan(),
            RatePlan.WEEKEND_PACKAGE, new WeekendPackagePlan(),
            RatePlan.LONG_STAY, new LongStayPlan());

    static RatePlanStrategy strategyFor(RatePlan plan) {
        return STRATEGIES.get(plan);
    }
}
