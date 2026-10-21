package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;

/** 15% off, but nothing comes back on cancellation. */
final class NonRefundablePlan implements RatePlanPolicy {

    static final BigDecimal DISCOUNT = new BigDecimal("0.15");

    @Override
    public Price price(Room room, NewReservation request, PriceCalculator calculator) {
        return calculator.discounted(room, request, DISCOUNT);
    }

    @Override
    public BigDecimal refund(BigDecimal amountPaid) {
        return PriceCalculator.cents(BigDecimal.ZERO);
    }
}
