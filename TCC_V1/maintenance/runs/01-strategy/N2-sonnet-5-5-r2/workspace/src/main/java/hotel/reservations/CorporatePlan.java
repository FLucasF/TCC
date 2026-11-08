package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;
import java.util.Map;

/** Agreement with companies: each one has its own discount, and the guest says which one with the company code. */
final class CorporatePlan implements RatePlanPolicy {

    private static final Map<String, BigDecimal> DISCOUNT_BY_COMPANY = Map.of(
            "TECNOSUL", new BigDecimal("0.12"),
            "AGROVALE", new BigDecimal("0.18"),
            "FARMAVIDA", new BigDecimal("0.25"));

    @Override
    public Price price(Room room, NewReservation request, Price base) {
        BigDecimal discount = request.companyCode() == null ? null : DISCOUNT_BY_COMPANY.get(request.companyCode());
        if (discount == null) {
            throw new HotelException(
                    ErrorCode.INVALID_REQUEST, "the corporate plan needs the code of a company with an agreement");
        }
        return RatePlanPolicy.discounted(base, discount);
    }

    @Override
    public BigDecimal refund(BigDecimal amountPaid) {
        return amountPaid;
    }
}
