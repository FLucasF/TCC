package hotel.reservations;

public enum RatePlan {
    /** Full price; the guest gets everything back when cancelling a confirmed reservation. */
    STANDARD(new StandardPlanStrategy(new PriceCalculator())),
    /** 15% off, but nothing comes back on cancellation. */
    NON_REFUNDABLE(new NonRefundablePlanStrategy(new PriceCalculator())),
    /** Corporate discount for partner companies. */
    CORPORATE(new CorporatePlanStrategy(new PriceCalculator())),
    /** Fixed price weekend package (Friday to Sunday). */
    WEEKEND_PACKAGE(new WeekendPackagePlanStrategy()),
    /** 10% discount for stays of 7 nights or more. */
    LONG_STAY(new LongStayPlanStrategy(new PriceCalculator()));

    private final RatePlanStrategy strategy;

    RatePlan(RatePlanStrategy strategy) {
        this.strategy = strategy;
    }

    public RatePlanStrategy strategy() {
        return strategy;
    }
}
