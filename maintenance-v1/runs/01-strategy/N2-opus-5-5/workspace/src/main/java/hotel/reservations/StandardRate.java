package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;

/** Full price; everything paid comes back on cancellation. */
final class StandardRate implements RatePlanPolicy {

    @Override
    public Price price(Room room, NewReservation request, PriceCalculator calculator) {
        return calculator.nightlyRates(room, request.checkIn(), request.checkOut());
    }

    @Override
    public BigDecimal refund(Reservation reservation) {
        return reservation.amountPaid();
    }
}
