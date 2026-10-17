package hotel.reservations;

import java.util.Map;

final class RatePlanStrategies {

    private static final Map<RatePlan, RatePlanStrategy> STRATEGIES = Map.of(
            RatePlan.STANDARD, new StandardStrategy(),
            RatePlan.NON_REFUNDABLE, new NonRefundableStrategy(),
            RatePlan.CORPORATE, new CorporateStrategy(),
            RatePlan.WEEKEND_PACKAGE, new WeekendPackageStrategy(),
            RatePlan.LONG_STAY, new LongStayStrategy());

    static RatePlanStrategy get(RatePlan ratePlan) {
        return STRATEGIES.get(ratePlan);
    }

    private RatePlanStrategies() {}
}
