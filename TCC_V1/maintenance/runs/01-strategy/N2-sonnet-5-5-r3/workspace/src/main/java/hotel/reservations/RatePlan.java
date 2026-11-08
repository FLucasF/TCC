package hotel.reservations;

/** The rate plans a guest can book; each one carries its own rules for price, eligibility and refund. */
public enum RatePlan {
    /** Full price; the guest gets everything back when cancelling a confirmed reservation. */
    STANDARD(new StandardRule()),
    /** 15% off, but nothing comes back on cancellation. */
    NON_REFUNDABLE(new NonRefundableRule()),
    /** Company agreement: a discount that depends on the company code; everything comes back on cancellation. */
    CORPORATE(new CorporateRule()),
    /** Friday to Sunday for a closed price all year round; half comes back on cancellation. */
    WEEKEND_PACKAGE(new WeekendPackageRule()),
    /** 7 nights or more with 10% off; everything comes back on cancellation. */
    LONG_STAY(new LongStayRule());

    private final RatePlanRule rule;

    RatePlan(RatePlanRule rule) {
        this.rule = rule;
    }

    RatePlanRule rule() {
        return rule;
    }
}
