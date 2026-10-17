package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;
import java.util.Map;

/**
 * The agreement with companies: the guest gives the company's code and gets the company's discount.
 * Everything paid comes back on cancellation.
 */
final class CorporateRate implements RatePlanPolicy {

    static final Map<String, BigDecimal> DISCOUNT_BY_COMPANY = Map.of(
            "TECNOSUL", new BigDecimal("0.12"),
            "AGROVALE", new BigDecimal("0.18"),
            "FARMAVIDA", new BigDecimal("0.25"));

    @Override
    public void checkEligibility(NewReservation request) {
        if (request.companyCode() == null || !DISCOUNT_BY_COMPANY.containsKey(request.companyCode())) {
            throw new HotelException(
                    ErrorCode.INVALID_REQUEST, "the CORPORATE rate plan needs the code of a partner company");
        }
    }

    @Override
    public Price price(Room room, NewReservation request, PriceCalculator calculator) {
        return calculator.nightlyRates(room, request.checkIn(), request.checkOut())
                .withDiscount(DISCOUNT_BY_COMPANY.get(request.companyCode()));
    }

    @Override
    public BigDecimal refund(Reservation reservation) {
        return reservation.amountPaid();
    }
}
