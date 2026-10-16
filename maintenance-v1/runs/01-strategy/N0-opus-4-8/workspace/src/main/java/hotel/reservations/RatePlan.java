package hotel.reservations;

/**
 * The rate plans the inn sells. Each one carries its {@link RatePlanPolicy}: how its stay is priced,
 * who may book it, and what comes back on cancellation. A new plan is a new constant with its policy.
 */
public enum RatePlan {
    /** Full price; everything comes back on cancellation. */
    STANDARD(new StandardPolicy()),
    /** 15% off, but nothing comes back on cancellation. */
    NON_REFUNDABLE(new NonRefundablePolicy()),
    /** A company deal: a per-company discount on the nights; everything comes back on cancellation. */
    CORPORATE(new CorporatePolicy()),
    /** Friday to the next Sunday, two nights, at a flat price; half comes back on cancellation. */
    WEEKEND_PACKAGE(new WeekendPackagePolicy()),
    /** Seven nights or more: 10% off the nights; everything comes back on cancellation. */
    LONG_STAY(new LongStayPolicy());

    private final RatePlanPolicy policy;

    RatePlan(RatePlanPolicy policy) {
        this.policy = policy;
    }

    public RatePlanPolicy policy() {
        return policy;
    }
}
