package hotel.reservations;

public enum RatePlan {
    STANDARD(new StandardRatePlan()),
    NON_REFUNDABLE(new NonRefundableRatePlan()),
    CORPORATE(new CorporateRatePlan()),
    WEEKEND_PACKAGE(new WeekendPackageRatePlan()),
    LONG_STAY(new LongStayRatePlan());

    private final RatePlanStrategy strategy;

    RatePlan(RatePlanStrategy strategy) {
        this.strategy = strategy;
    }

    public RatePlanStrategy strategy() {
        return strategy;
    }
}
