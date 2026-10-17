package hotel.reservations;

public enum RatePlan {
    /** Full price; the guest gets everything back when cancelling a confirmed reservation. */
    STANDARD(new StandardPolicy()),
    /** 15% off, but nothing comes back on cancellation. */
    NON_REFUNDABLE(new NonRefundablePolicy()),
    /** Discount by company agreement, given the company code; everything comes back on cancellation. */
    CORPORATE(new CorporatePolicy()),
    /** Friday to Sunday for a closed price; half comes back on cancellation. */
    WEEKEND_PACKAGE(new WeekendPackagePolicy()),
    /** 10% off for 7 nights or more; everything comes back on cancellation. */
    LONG_STAY(new LongStayPolicy());

    private final RatePlanPolicy policy;

    RatePlan(RatePlanPolicy policy) {
        this.policy = policy;
    }

    public RatePlanPolicy policy() {
        return policy;
    }
}
