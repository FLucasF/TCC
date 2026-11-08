package hotel.reservations;

import java.util.Map;

final class RatePlanStrategies {

    private static final Map<RatePlan, RatePlanStrategy> STRATEGIES = Map.of(
            RatePlan.STANDARD, new StandardRatePlan(),
            RatePlan.NON_REFUNDABLE, new NonRefundableRatePlan(),
            RatePlan.CORPORATE, new CorporateRatePlan(),
            RatePlan.WEEKEND_PACKAGE, new WeekendPackageRatePlan(),
            RatePlan.LONG_STAY, new LongStayRatePlan());

    static RatePlanStrategy of(RatePlan ratePlan) {
        return STRATEGIES.get(ratePlan);
    }

    private RatePlanStrategies() {}
}
