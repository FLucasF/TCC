package hotel.reservations;

/** The rate plans a reservation can use; each one carries its own rules. */
public enum RatePlan {
    /** Full price; the guest gets everything back when cancelling a confirmed reservation. */
    STANDARD(new StandardPlan()),
    /** 15% off, but nothing comes back on cancellation. */
    NON_REFUNDABLE(new NonRefundablePlan()),
    /** Discount agreed with a company, given by the company code; everything comes back on cancellation. */
    CORPORATE(new CorporatePlan()),
    /** Friday to Sunday for a closed price; half comes back on cancellation. */
    WEEKEND_PACKAGE(new WeekendPackagePlan()),
    /** 10% off for 7 nights or more; everything comes back on cancellation. */
    LONG_STAY(new LongStayPlan());

    private final RatePlanPolicy policy;

    RatePlan(RatePlanPolicy policy) {
        this.policy = policy;
    }

    RatePlanPolicy policy() {
        return policy;
    }
}
