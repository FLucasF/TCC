package hotel.reservations.plans;

import hotel.reservations.ErrorCode;
import hotel.reservations.HotelException;
import hotel.reservations.NewReservation;
import hotel.reservations.Price;
import hotel.rooms.Room;
import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;

/** Seven nights or more: 10% off the nights (high-season surcharge still applies). */
public final class LongStayPolicy implements RatePlanPolicy {

    private static final BigDecimal DISCOUNT = new BigDecimal("0.10");
    private static final int MINIMUM_NIGHTS = 7;

    @Override
    public void validate(NewReservation request) {
        if (ChronoUnit.DAYS.between(request.checkIn(), request.checkOut()) < MINIMUM_NIGHTS) {
            throw new HotelException(
                    ErrorCode.INVALID_REQUEST,
                    "the long-stay rate plan needs at least " + MINIMUM_NIGHTS + " nights");
        }
    }

    @Override
    public Price price(Room room, NewReservation request, PricingSupport pricing) {
        int nights = pricing.nights(request.checkIn(), request.checkOut());
        BigDecimal subtotal = pricing.subtotal(room, request.checkIn(), request.checkOut());
        BigDecimal discount = pricing.cents(subtotal.multiply(DISCOUNT));
        return new Price(nights, subtotal, discount, subtotal.subtract(discount));
    }

    @Override
    public BigDecimal refund(BigDecimal amountPaid) {
        return amountPaid;
    }
}
