package hotel.reservations;

import java.util.Map;

final class RatePlanStrategies {

    private static final Map<RatePlan, RatePlanStrategy> STRATEGIES = Map.of(
            RatePlan.STANDARD, new StandardRatePlanStrategy(),
            RatePlan.NON_REFUNDABLE, new NonRefundableRatePlanStrategy(),
            RatePlan.CORPORATE, new CorporateRatePlanStrategy(),
            RatePlan.WEEKEND_PACKAGE, new WeekendPackageRatePlanStrategy(),
            RatePlan.LONG_STAY, new LongStayRatePlanStrategy()
    );

    static RatePlanStrategy get(RatePlan ratePlan) {
        return STRATEGIES.get(ratePlan);
    }
}
