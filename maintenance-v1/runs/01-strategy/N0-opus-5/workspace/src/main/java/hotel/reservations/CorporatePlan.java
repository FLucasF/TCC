package hotel.reservations;

import java.math.BigDecimal;
import java.util.Map;

/**
 * The company deal: each company has its own discount on the nights, and the guest has to inform the
 * code of a company we have a deal with. Cancelling gives everything back.
 */
final class CorporatePlan extends NightlyDiscountPlan {

    /** The companies we have a deal with, by the code the guest informs, and their discounts. */
    static final Map<String, BigDecimal> COMPANY_DISCOUNTS = Map.of(
            "TECNOSUL", new BigDecimal("0.12"),
            "AGROVALE", new BigDecimal("0.18"),
            "FARMAVIDA", new BigDecimal("0.25"));

    @Override
    public void checkEligibility(Stay stay) {
        if (stay.companyCode() == null || !COMPANY_DISCOUNTS.containsKey(stay.companyCode())) {
            throw RatePlanPolicy.notEligible(
                    "the corporate rate needs the code of a company we have a deal with");
        }
    }

    @Override
    BigDecimal discountRate(Stay stay) {
        checkEligibility(stay);
        return COMPANY_DISCOUNTS.get(stay.companyCode());
    }
}
