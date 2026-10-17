package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;

/** Full price; the guest gets everything back on cancellation. */
final class StandardPlan implements RatePlanRules {

    @Override
    public Price price(NewReservation request, Room room, Price seasonalPrice) {
        return seasonalPrice;
    }

    @Override
    public BigDecimal refund(BigDecimal amountPaid) {
        return amountPaid;
    }
}
