package hotel.reservations.plans;

import hotel.reservations.NewReservation;
import hotel.reservations.Price;
import hotel.rooms.Room;
import java.math.BigDecimal;

/** Full price; the guest gets everything back when cancelling a confirmed reservation. */
public final class StandardPolicy implements RatePlanPolicy {

    @Override
    public void validate(NewReservation request) {
    }

    @Override
    public Price price(Room room, NewReservation request, PricingSupport pricing) {
        int nights = pricing.nights(request.checkIn(), request.checkOut());
        BigDecimal subtotal = pricing.subtotal(room, request.checkIn(), request.checkOut());
        BigDecimal zero = pricing.cents(BigDecimal.ZERO);
        return new Price(nights, subtotal, zero, subtotal);
    }

    @Override
    public BigDecimal refund(BigDecimal amountPaid) {
        return amountPaid;
    }
}
