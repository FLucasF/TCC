package hotel.reservations;

import java.util.EnumMap;
import java.util.Map;

public final class RatePlanStrategies {

    private final Map<RatePlan, RatePlanStrategy> strategies;

    public RatePlanStrategies() {
        this.strategies = new EnumMap<>(RatePlan.class);
        this.strategies.put(RatePlan.STANDARD, new StandardStrategy());
        this.strategies.put(RatePlan.NON_REFUNDABLE, new NonRefundableStrategy());
        this.strategies.put(RatePlan.CORPORATE, new CorporateStrategy());
        this.strategies.put(RatePlan.WEEKEND_PACKAGE, new WeekendPackageStrategy());
        this.strategies.put(RatePlan.LONG_STAY, new LongStayStrategy());
    }

    public RatePlanStrategy get(RatePlan ratePlan) {
        return strategies.get(ratePlan);
    }
}
