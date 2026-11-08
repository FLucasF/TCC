package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;

final class NonRefundablePlan implements RatePlanPolicy {

    static final BigDecimal DISCOUNT = new BigDecimal("0.15");

    @Override
    public Price price(Room room, NewReservation request, Price base) {
        return RatePlanPolicy.discounted(base, DISCOUNT);
    }

    @Override
    public BigDecimal refund(BigDecimal amountPaid) {
        return PriceCalculator.cents(BigDecimal.ZERO);
    }
}
