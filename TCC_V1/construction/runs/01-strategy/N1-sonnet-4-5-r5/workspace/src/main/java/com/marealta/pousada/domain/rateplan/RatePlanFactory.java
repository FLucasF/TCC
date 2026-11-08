package com.marealta.pousada.domain.rateplan;

import com.marealta.pousada.exception.InvalidRequestException;

public class RatePlanFactory {

    public static RatePlan create(String ratePlanName) {
        if (ratePlanName == null || ratePlanName.isBlank()) {
            throw new InvalidRequestException("Rate plan is required");
        }

        return switch (ratePlanName) {
            case "STANDARD" -> new StandardRatePlan();
            case "NON_REFUNDABLE" -> new NonRefundableRatePlan();
            case "CORPORATE" -> new CorporateRatePlan();
            case "WEEKEND_PACKAGE" -> new WeekendPackageRatePlan();
            case "LONG_STAY" -> new LongStayRatePlan();
            default -> throw new InvalidRequestException("Invalid rate plan: " + ratePlanName);
        };
    }
}
