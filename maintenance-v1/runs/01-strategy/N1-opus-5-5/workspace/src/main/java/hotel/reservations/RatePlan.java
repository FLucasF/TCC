package hotel.reservations;

/** The rate plans; what each one means lives in its rules. */
public enum RatePlan {
    STANDARD(new StandardPlan()),
    NON_REFUNDABLE(new NonRefundablePlan()),
    CORPORATE(new CorporatePlan()),
    WEEKEND_PACKAGE(new WeekendPackagePlan()),
    LONG_STAY(new LongStayPlan());

    private final RatePlanRules rules;

    RatePlan(RatePlanRules rules) {
        this.rules = rules;
    }

    RatePlanRules rules() {
        return rules;
    }
}
