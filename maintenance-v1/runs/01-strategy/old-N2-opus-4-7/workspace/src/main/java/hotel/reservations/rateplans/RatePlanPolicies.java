package hotel.reservations.rateplans;

import hotel.reservations.RatePlan;
import java.util.EnumMap;
import java.util.Map;

/** Registry: tells which {@link RatePlanPolicy} runs for each {@link RatePlan}. */
public final class RatePlanPolicies {

    private final Map<RatePlan, RatePlanPolicy> policies = new EnumMap<>(RatePlan.class);

    public RatePlanPolicies() {
        policies.put(RatePlan.STANDARD, new StandardPolicy());
        policies.put(RatePlan.NON_REFUNDABLE, new NonRefundablePolicy());
        policies.put(RatePlan.CORPORATE, new CorporatePolicy());
        policies.put(RatePlan.WEEKEND_PACKAGE, new WeekendPackagePolicy());
        policies.put(RatePlan.LONG_STAY, new LongStayPolicy());
    }

    public RatePlanPolicy of(RatePlan plan) {
        return policies.get(plan);
    }
}
