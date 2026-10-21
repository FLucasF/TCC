package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;

/** Full price; the guest gets everything back when cancelling. */
final class StandardPlan implements RatePlanPolicy {

    @Override
    public Price price(Room room, NewReservation request, PriceCalculator calculator) {
        return calculator.discounted(room, request, BigDecimal.ZERO);
    }

    @Override
    public BigDecimal refund(BigDecimal amountPaid) {
        return amountPaid;
    }
}
