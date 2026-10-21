package hotel.reservations;

import java.util.EnumMap;
import java.util.Map;

/** Maps each rate plan to how it works. Adding a plan: add the enum value, write the strategy, wire it here. */
final class RatePlanStrategies {

    private static final Map<RatePlan, RatePlanStrategy> STRATEGIES = new EnumMap<>(RatePlan.class);

    static {
        STRATEGIES.put(RatePlan.STANDARD, new StandardRatePlanStrategy());
        STRATEGIES.put(RatePlan.NON_REFUNDABLE, new NonRefundableRatePlanStrategy());
        STRATEGIES.put(RatePlan.CORPORATE, new CorporateRatePlanStrategy());
        STRATEGIES.put(RatePlan.WEEKEND_PACKAGE, new WeekendPackageRatePlanStrategy());
        STRATEGIES.put(RatePlan.LONG_STAY, new LongStayRatePlanStrategy());
    }

    static RatePlanStrategy get(RatePlan ratePlan) {
        return STRATEGIES.get(ratePlan);
    }
}
