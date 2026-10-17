package hotel.reservations;

import java.math.BigDecimal;
import java.util.Map;

/**
 * The company agreement: each company has the discount it settled with the inn, and the guest informs
 * its code when booking. On cancellation everything comes back, as in the standard plan.
 */
final class CorporatePlan implements RatePlanPolicy {

    private static final Map<String, BigDecimal> DISCOUNT_BY_COMPANY = Map.of(
            "TECNOSUL", new BigDecimal("0.12"),
            "AGROVALE", new BigDecimal("0.18"),
            "FARMAVIDA", new BigDecimal("0.25"));

    @Override
    public void checkBookable(NewReservation request) {
        discountOf(request.companyCode());
    }

    @Override
    public Price price(Stay stay) {
        return Price.percentOff(
                stay.nights(), stay.nightsSubtotal(), discountOf(stay.request().companyCode()));
    }

    @Override
    public BigDecimal refund(BigDecimal amountPaid) {
        return amountPaid;
    }

    private static BigDecimal discountOf(String companyCode) {
        BigDecimal discount = companyCode == null ? null : DISCOUNT_BY_COMPANY.get(companyCode);
        if (discount == null) {
            throw new HotelException(
                    ErrorCode.INVALID_REQUEST,
                    "the corporate plan needs the code of a company with an agreement");
        }
        return discount;
    }
}
