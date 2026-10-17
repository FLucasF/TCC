package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;

/** 15% off, but nothing comes back on cancellation. */
final class NonRefundablePlan implements RatePlanRules {

    static final BigDecimal DISCOUNT = new BigDecimal("0.15");

    @Override
    public Price price(NewReservation request, Room room, Price seasonalPrice) {
        return seasonalPrice.withDiscount(DISCOUNT);
    }

    @Override
    public BigDecimal refund(BigDecimal amountPaid) {
        return PriceCalculator.cents(BigDecimal.ZERO);
    }
}
