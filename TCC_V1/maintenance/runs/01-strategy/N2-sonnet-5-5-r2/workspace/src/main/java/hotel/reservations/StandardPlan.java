package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;

final class StandardPlan implements RatePlanPolicy {

    @Override
    public Price price(Room room, NewReservation request, Price base) {
        return base;
    }

    @Override
    public BigDecimal refund(BigDecimal amountPaid) {
        return amountPaid;
    }
}
