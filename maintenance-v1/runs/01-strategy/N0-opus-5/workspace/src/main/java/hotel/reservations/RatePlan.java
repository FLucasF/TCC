package hotel.reservations;

/**
 * The rate plans the inn sells. Each one carries its own policy: its price, who may book it, and what
 * comes back on cancellation.
 */
public enum RatePlan {
    /** Full price; the guest gets everything back when cancelling a confirmed reservation. */
    STANDARD(new StandardPlan()),
    /** 15% off, but nothing comes back on cancellation. */
    NON_REFUNDABLE(new NonRefundablePlan()),
    /** The company deal: the company's own discount, for whoever informs its code. */
    CORPORATE(new CorporatePlan()),
    /** Friday to Sunday for a closed price; half of what was paid comes back. */
    WEEKEND_PACKAGE(new WeekendPackagePlan()),
    /** 10% off for seven nights or more. */
    LONG_STAY(new LongStayPlan());

    private final RatePlanPolicy policy;

    RatePlan(RatePlanPolicy policy) {
        this.policy = policy;
    }

    public RatePlanPolicy policy() {
        return policy;
    }
}
