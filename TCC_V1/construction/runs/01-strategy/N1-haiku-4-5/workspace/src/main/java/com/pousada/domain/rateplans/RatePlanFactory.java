package com.pousada.domain.rateplans;

public class RatePlanFactory {
    public static RatePlan create(String planName, String companyCode) throws IllegalArgumentException {
        return switch (planName) {
            case "STANDARD" -> new StandardPlan();
            case "NON_REFUNDABLE" -> new NonRefundablePlan();
            case "CORPORATE" -> new CorporatePlan(companyCode);
            case "WEEKEND_PACKAGE" -> new WeekendPackagePlan();
            case "LONG_STAY" -> new LongStayPlan();
            default -> throw new IllegalArgumentException("Unknown rate plan: " + planName);
        };
    }
}
