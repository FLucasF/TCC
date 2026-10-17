package hotel.reservations;

/** The plans a stay can be booked under. Each one knows its own policy. */
public enum RatePlan {
    /** Full price; the guest gets everything back when cancelling a confirmed reservation. */
    STANDARD(new StandardPlan()),
    /** 15% off, but nothing comes back on cancellation. */
    NON_REFUNDABLE(new NonRefundablePlan()),
    /** The discount agreed with the guest's company; everything comes back on cancellation. */
    CORPORATE(new CorporatePlan()),
    /** Friday to Sunday for a closed price; half comes back on cancellation. */
    WEEKEND_PACKAGE(new WeekendPackagePlan()),
    /** 10% off from seven nights on; everything comes back on cancellation. */
    LONG_STAY(new LongStayPlan());

    private final RatePlanPolicy policy;

    RatePlan(RatePlanPolicy policy) {
        this.policy = policy;
    }

    /** How this plan prices a stay, who may use it, and what comes back on cancellation. */
    public RatePlanPolicy policy() {
        return policy;
    }
}
