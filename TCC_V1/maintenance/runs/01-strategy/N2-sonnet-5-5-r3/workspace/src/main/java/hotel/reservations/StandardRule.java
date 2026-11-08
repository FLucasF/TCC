package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;

final class StandardRule implements RatePlanRule {

    @Override
    public Price price(Room room, NewReservation request, Price undiscounted) {
        return undiscounted;
    }

    @Override
    public BigDecimal refund(Reservation reservation) {
        return reservation.amountPaid();
    }
}
