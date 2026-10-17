package hotel.reservations.plans;

import hotel.reservations.ErrorCode;
import hotel.reservations.HotelException;
import hotel.reservations.NewReservation;
import hotel.reservations.Price;
import hotel.rooms.Room;
import java.math.BigDecimal;
import java.util.Map;

/** Corporate convenience: the guest's company has an agreed discount on the nights. */
public final class CorporatePolicy implements RatePlanPolicy {

    private static final Map<String, BigDecimal> DISCOUNTS = Map.of(
            "TECNOSUL", new BigDecimal("0.12"),
            "AGROVALE", new BigDecimal("0.18"),
            "FARMAVIDA", new BigDecimal("0.25"));

    @Override
    public void validate(NewReservation request) {
        if (request.companyCode() == null || !DISCOUNTS.containsKey(request.companyCode())) {
            throw new HotelException(
                    ErrorCode.INVALID_REQUEST,
                    "the corporate rate plan requires a known company code");
        }
    }

    @Override
    public Price price(Room room, NewReservation request, PricingSupport pricing) {
        int nights = pricing.nights(request.checkIn(), request.checkOut());
        BigDecimal subtotal = pricing.subtotal(room, request.checkIn(), request.checkOut());
        BigDecimal discount = pricing.cents(subtotal.multiply(DISCOUNTS.get(request.companyCode())));
        return new Price(nights, subtotal, discount, subtotal.subtract(discount));
    }

    @Override
    public BigDecimal refund(BigDecimal amountPaid) {
        return amountPaid;
    }
}
