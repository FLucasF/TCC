package hotel.reservations;

/** The rate plans, each with its own rule; a new plan is a constant here and a {@link RatePlanRule}. */
public enum RatePlan {
    /** Full price; the guest gets everything back when cancelling a confirmed reservation. */
    STANDARD(new StandardRule()),
    /** 15% off, but nothing comes back on cancellation. */
    NON_REFUNDABLE(new NonRefundableRule()),
    /** Discount by company, given the company code; everything comes back on cancellation. */
    CORPORATE(new CorporateRule()),
    /** Friday to Sunday at a closed price; half comes back on cancellation. */
    WEEKEND_PACKAGE(new WeekendPackageRule()),
    /** 10% off for seven nights or more; everything comes back on cancellation. */
    LONG_STAY(new LongStayRule());

    private final RatePlanRule rule;

    RatePlan(RatePlanRule rule) {
        this.rule = rule;
    }

    RatePlanRule rule() {
        return rule;
    }
}
