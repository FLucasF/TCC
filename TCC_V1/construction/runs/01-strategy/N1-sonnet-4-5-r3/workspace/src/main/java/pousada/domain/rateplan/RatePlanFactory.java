package pousada.domain.rateplan;

import java.util.Map;

public class RatePlanFactory {

    private static final Map<String, RatePlan> PLANS = Map.of(
        "STANDARD", new StandardRatePlan(),
        "NON_REFUNDABLE", new NonRefundableRatePlan(),
        "CORPORATE", new CorporateRatePlan(),
        "WEEKEND_PACKAGE", new WeekendPackageRatePlan(),
        "LONG_STAY", new LongStayRatePlan()
    );

    public static RatePlan get(String ratePlanName) {
        RatePlan plan = PLANS.get(ratePlanName);
        if (plan == null) {
            throw new IllegalArgumentException("Unknown rate plan: " + ratePlanName);
        }
        return plan;
    }
}
