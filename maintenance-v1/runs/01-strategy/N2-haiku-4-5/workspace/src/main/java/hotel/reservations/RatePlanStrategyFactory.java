package hotel.reservations;

public class RatePlanStrategyFactory {

    private final PriceCalculator priceCalculator;

    public RatePlanStrategyFactory(PriceCalculator priceCalculator) {
        this.priceCalculator = priceCalculator;
    }

    public RatePlanStrategy createStrategy(RatePlan ratePlan, String companyCode) {
        return switch (ratePlan) {
            case STANDARD -> new StandardStrategy(priceCalculator);
            case NON_REFUNDABLE -> new NonRefundableStrategy(priceCalculator);
            case CORPORATE -> {
                if (companyCode == null || companyCode.isBlank()) {
                    throw new HotelException(
                            ErrorCode.INVALID_REQUEST, "corporate plan requires a company code");
                }
                yield new CorporateStrategy(priceCalculator, companyCode);
            }
            case WEEKEND_PACKAGE -> new WeekendPackageStrategy();
            case LONG_STAY -> new LongStayStrategy(priceCalculator);
        };
    }
}
