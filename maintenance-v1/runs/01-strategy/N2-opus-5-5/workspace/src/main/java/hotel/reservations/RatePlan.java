package hotel.reservations;

/** The rate plans the guest can choose; each one brings its own rules (see {@link RatePlanPolicy}). */
public enum RatePlan {
    STANDARD(new StandardRate()),
    NON_REFUNDABLE(new NonRefundableRate()),
    CORPORATE(new CorporateRate()),
    WEEKEND_PACKAGE(new WeekendPackageRate()),
    LONG_STAY(new LongStayRate());

    private final RatePlanPolicy policy;

    RatePlan(RatePlanPolicy policy) {
        this.policy = policy;
    }

    RatePlanPolicy policy() {
        return policy;
    }
}
