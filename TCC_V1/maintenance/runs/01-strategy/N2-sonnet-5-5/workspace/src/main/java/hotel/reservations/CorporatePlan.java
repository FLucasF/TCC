package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;
import java.util.Map;

/** A discount agreed with a company, identified by the company code; everything comes back on cancellation. */
final class CorporatePlan implements RatePlanPolicy {

    private static final Map<String, BigDecimal> DISCOUNT_BY_COMPANY = Map.of(
            "TECNOSUL", new BigDecimal("0.12"),
            "AGROVALE", new BigDecimal("0.18"),
            "FARMAVIDA", new BigDecimal("0.25"));

    @Override
    public void validate(NewReservation request) {
        if (request.companyCode() == null || !DISCOUNT_BY_COMPANY.containsKey(request.companyCode())) {
            throw new HotelException(
                    ErrorCode.INVALID_REQUEST, "the CORPORATE plan needs the code of a company with an agreement");
        }
    }

    @Override
    public Price price(Room room, NewReservation request, PriceCalculator calculator) {
        return calculator.discounted(room, request, DISCOUNT_BY_COMPANY.get(request.companyCode()));
    }

    @Override
    public BigDecimal refund(BigDecimal amountPaid) {
        return amountPaid;
    }
}
