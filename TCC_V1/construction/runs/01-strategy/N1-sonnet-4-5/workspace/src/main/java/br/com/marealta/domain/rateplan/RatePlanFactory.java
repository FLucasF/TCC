package br.com.marealta.domain.rateplan;

public class RatePlanFactory {

    public static RatePlan create(String type) {
        return switch (type) {
            case "STANDARD" -> new StandardRatePlan();
            case "NON_REFUNDABLE" -> new NonRefundableRatePlan();
            case "CORPORATE" -> new CorporateRatePlan();
            case "WEEKEND_PACKAGE" -> new WeekendPackageRatePlan();
            case "LONG_STAY" -> new LongStayRatePlan();
            default -> throw new IllegalArgumentException("Unknown rate plan type: " + type);
        };
    }
}
