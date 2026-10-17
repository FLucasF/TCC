package hotel.reservations;

public enum RatePlan {
    /** Full price; the guest gets everything back when cancelling a confirmed reservation. */
    STANDARD(new StandardPlan()),
    /** 15% off, but nothing comes back on cancellation. */
    NON_REFUNDABLE(new NonRefundablePlan()),
    /** The partner companies' discount; the site sends the company's name in capitals as the code. */
    CORPORATE(new CorporatePlan()),
    /** Friday to Sunday, two nights, a fixed price per room type; half comes back on cancellation. */
    WEEKEND_PACKAGE(new WeekendPackagePlan()),
    /** 10% off for stays of 7 nights or more; everything comes back on cancellation. */
    LONG_STAY(new LongStayPlan());

    private final RatePlanRules rules;

    RatePlan(RatePlanRules rules) {
        this.rules = rules;
    }

    RatePlanRules rules() {
        return rules;
    }
}
