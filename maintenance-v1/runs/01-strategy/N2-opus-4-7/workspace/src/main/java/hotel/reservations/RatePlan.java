package hotel.reservations;

import hotel.reservations.plans.CorporatePolicy;
import hotel.reservations.plans.LongStayPolicy;
import hotel.reservations.plans.NonRefundablePolicy;
import hotel.reservations.plans.RatePlanPolicy;
import hotel.reservations.plans.StandardPolicy;
import hotel.reservations.plans.WeekendPackagePolicy;

public enum RatePlan {
    STANDARD(new StandardPolicy()),
    NON_REFUNDABLE(new NonRefundablePolicy()),
    CORPORATE(new CorporatePolicy()),
    WEEKEND_PACKAGE(new WeekendPackagePolicy()),
    LONG_STAY(new LongStayPolicy());

    private final RatePlanPolicy policy;

    RatePlan(RatePlanPolicy policy) {
        this.policy = policy;
    }

    public RatePlanPolicy policy() {
        return policy;
    }
}
