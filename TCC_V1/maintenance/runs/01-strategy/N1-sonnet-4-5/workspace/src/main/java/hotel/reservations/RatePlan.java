package hotel.reservations;

public enum RatePlan {
    STANDARD(new StandardPricing()),
    NON_REFUNDABLE(new NonRefundablePricing()),
    CORPORATE(new CorporatePricing()),
    WEEKEND_PACKAGE(new WeekendPackagePricing()),
    LONG_STAY(new LongStayPricing());

    private final RatePlanPricing pricing;

    RatePlan(RatePlanPricing pricing) {
        this.pricing = pricing;
    }

    RatePlanPricing pricing() {
        return pricing;
    }
}
