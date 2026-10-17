package hotel.reservations;

import java.math.BigDecimal;
import java.util.Map;

/**
 * The agreement with the companies: each one has its own discount on the nights, and the guest tells
 * us the company by its code. Without a code we know, the stay cannot be booked under this plan.
 * Everything paid comes back on cancellation.
 */
public final class CorporatePlan implements RatePlanPolicy {

    private static final Map<String, BigDecimal> DISCOUNT_BY_COMPANY = Map.of(
            "TECNOSUL", new BigDecimal("0.12"),
            "AGROVALE", new BigDecimal("0.18"),
            "FARMAVIDA", new BigDecimal("0.25"));

    @Override
    public void validate(Stay stay) {
        discountFor(stay);
    }

    @Override
    public Price price(Stay stay) {
        return PriceCalculator.nightsWithDiscount(stay, discountFor(stay));
    }

    @Override
    public BigDecimal refund(BigDecimal amountPaid) {
        return amountPaid;
    }

    private static BigDecimal discountFor(Stay stay) {
        BigDecimal discount = stay.companyCode() == null ? null : DISCOUNT_BY_COMPANY.get(stay.companyCode());
        if (discount == null) {
            throw new HotelException(
                    ErrorCode.INVALID_REQUEST,
                    "the corporate plan needs the code of a company that has an agreement with us");
        }
        return discount;
    }
}
