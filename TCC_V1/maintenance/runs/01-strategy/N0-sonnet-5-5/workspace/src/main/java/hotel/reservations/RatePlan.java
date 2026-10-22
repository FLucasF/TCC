package hotel.reservations;

public enum RatePlan {
    /** Full price; the guest gets everything back when cancelling a confirmed reservation. */
    STANDARD(RatePlanPolicies.standard()),
    /** 15% off, but nothing comes back on cancellation. */
    NON_REFUNDABLE(RatePlanPolicies.nonRefundable()),
    /** Discount by partner company (needs the company code); everything comes back on cancellation. */
    CORPORATE(RatePlanPolicies.corporate()),
    /** Fixed price for two nights, Friday to Sunday; half comes back on cancellation. */
    WEEKEND_PACKAGE(RatePlanPolicies.weekendPackage()),
    /** 10% off for 7 nights or more; everything comes back on cancellation. */
    LONG_STAY(RatePlanPolicies.longStay());

    private final RatePlanPolicy policy;

    RatePlan(RatePlanPolicy policy) {
        this.policy = policy;
    }

    RatePlanPolicy policy() {
        return policy;
    }
}
