package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;
import java.util.Map;

/**
 * The agreement with companies: the guest gives the company's code and gets the discount the company
 * agreed on. Everything comes back on cancellation.
 */
final class CorporatePlan implements RatePlanRules {

    static final Map<String, BigDecimal> DISCOUNT_BY_COMPANY = Map.of(
            "TECNOSUL", new BigDecimal("0.12"),
            "AGROVALE", new BigDecimal("0.18"),
            "FARMAVIDA", new BigDecimal("0.25"));

    @Override
    public void checkEligibility(NewReservation request) {
        if (request.companyCode() == null || !DISCOUNT_BY_COMPANY.containsKey(request.companyCode())) {
            throw new HotelException(
                    ErrorCode.INVALID_REQUEST, "the CORPORATE plan needs the code of a company in the agreement");
        }
    }

    @Override
    public Price price(NewReservation request, Room room, Price seasonalPrice) {
        return seasonalPrice.withDiscount(DISCOUNT_BY_COMPANY.get(request.companyCode()));
    }

    @Override
    public BigDecimal refund(BigDecimal amountPaid) {
        return amountPaid;
    }
}
