package hotel.reservations;

import java.math.BigDecimal;
import java.util.Map;

/**
 * The agreement with companies: the guest gives the company's code and gets the discount that company
 * agreed with the inn. Everything comes back on cancellation, as in the standard plan.
 */
final class CorporatePlan extends DiscountedNightsPlan {

    /** The discount of each company, by the code the guest gives (the company's name in capitals). */
    private static final Map<String, BigDecimal> DISCOUNT_BY_COMPANY = Map.of(
            "TECNOSUL", new BigDecimal("0.12"),
            "AGROVALE", new BigDecimal("0.18"),
            "FARMAVIDA", new BigDecimal("0.25"));

    @Override
    public void checkEligibility(Stay stay) {
        if (stay.companyCode() == null || !DISCOUNT_BY_COMPANY.containsKey(stay.companyCode())) {
            throw new HotelException(
                    ErrorCode.INVALID_REQUEST,
                    "the corporate plan needs the code of a company with an agreement");
        }
    }

    @Override
    BigDecimal discountRate(Stay stay) {
        return DISCOUNT_BY_COMPANY.get(stay.companyCode());
    }

    @Override
    public BigDecimal refund(BigDecimal amountPaid) {
        return amountPaid;
    }
}
