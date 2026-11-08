package hotel.reservations;

public enum RatePlan {
    /** Full price; the guest gets everything back when cancelling a confirmed reservation. */
    STANDARD(new StandardRatePlanStrategy()),
    /** 15% off, but nothing comes back on cancellation. */
    NON_REFUNDABLE(new NonRefundableRatePlanStrategy()),
    /** Corporate partnership: companies get a discount based on their agreement. */
    CORPORATE(new CorporateRatePlanStrategy()),
    /** Weekend package: Friday check-in, Sunday check-out, fixed price per room type. */
    WEEKEND_PACKAGE(new WeekendPackageRatePlanStrategy()),
    /** Long stay: 7 nights or more, 10% discount, full refund on cancellation. */
    LONG_STAY(new LongStayRatePlanStrategy());

    private final RatePlanStrategy strategy;

    RatePlan(RatePlanStrategy strategy) {
        this.strategy = strategy;
    }

    public RatePlanStrategy strategy() {
        return strategy;
    }
}
