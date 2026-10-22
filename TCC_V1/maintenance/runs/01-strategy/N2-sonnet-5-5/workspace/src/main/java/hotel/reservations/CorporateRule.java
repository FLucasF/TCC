package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;
import java.util.Map;

/** The company agreements: each company has its own discount, and the guest says which one with the company code. */
final class CorporateRule implements RatePlanRule {

    private static final Map<String, BigDecimal> DISCOUNT_BY_COMPANY = Map.of(
            "TECNOSUL", new BigDecimal("0.12"),
            "AGROVALE", new BigDecimal("0.18"),
            "FARMAVIDA", new BigDecimal("0.25"));

    @Override
    public Price price(Room room, NewReservation request, Price seasonal) {
        BigDecimal discountRate = request.companyCode() == null ? null : DISCOUNT_BY_COMPANY.get(request.companyCode());
        if (discountRate == null) {
            throw new HotelException(ErrorCode.INVALID_REQUEST, "the rate plan CORPORATE needs a valid company code");
        }
        return seasonal.withDiscount(discountRate);
    }

    @Override
    public BigDecimal refund(BigDecimal amountPaid) {
        return amountPaid;
    }
}
