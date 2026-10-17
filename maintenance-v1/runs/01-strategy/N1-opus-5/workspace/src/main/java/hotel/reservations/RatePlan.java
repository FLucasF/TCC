package hotel.reservations;

/**
 * The plans the guest can book under. Each one brings its own way of pricing the stay, its own rules
 * about who may book it, and its own refund on cancellation: that is what its policy holds.
 */
public enum RatePlan {
    /** Full price; the guest gets everything back when cancelling a confirmed reservation. */
    STANDARD(new StandardPlan()),
    /** 15% off, but nothing comes back on cancellation. */
    NON_REFUNDABLE(new NonRefundablePlan()),
    /** The company agreement: the discount of the guest's company, everything back on cancellation. */
    CORPORATE(new CorporatePlan()),
    /** Friday to Sunday at a closed price; half of what was paid comes back on cancellation. */
    WEEKEND_PACKAGE(new WeekendPackagePlan()),
    /** Seven nights or more: 10% off, everything back on cancellation. */
    LONG_STAY(new LongStayPlan());

    private final RatePlanPolicy policy;

    RatePlan(RatePlanPolicy policy) {
        this.policy = policy;
    }

    public RatePlanPolicy policy() {
        return policy;
    }
}
