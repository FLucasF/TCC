package hotel.reservations;

import java.util.EnumMap;
import java.util.Map;

/** The policy for each rate plan. */
public final class RatePlanPolicies {

    private final Map<RatePlan, RatePlanPolicy> policies;

    public RatePlanPolicies() {
        PriceCalculator priceCalculator = new PriceCalculator();
        this.policies = new EnumMap<>(RatePlan.class);
        policies.put(RatePlan.STANDARD, new StandardPolicy(priceCalculator));
        policies.put(RatePlan.NON_REFUNDABLE, new NonRefundablePolicy(priceCalculator));
        policies.put(RatePlan.CORPORATE, new CorporatePolicy(priceCalculator));
        policies.put(RatePlan.WEEKEND_PACKAGE, new WeekendPackagePolicy());
        policies.put(RatePlan.LONG_STAY, new LongStayPolicy(priceCalculator));
    }

    public RatePlanPolicy of(RatePlan ratePlan) {
        RatePlanPolicy policy = policies.get(ratePlan);
        if (policy == null) {
            throw new HotelException(ErrorCode.INVALID_REQUEST, "unknown rate plan: " + ratePlan);
        }
        return policy;
    }
}
