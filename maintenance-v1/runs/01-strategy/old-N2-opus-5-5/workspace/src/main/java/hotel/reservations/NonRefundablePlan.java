package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;

/** 15% off, for anyone, but nothing comes back on cancellation. */
final class NonRefundablePlan implements RatePlanRules {

    static final BigDecimal DISCOUNT = new BigDecimal("0.15");

    @Override
    public void checkEligibility(NewReservation request) {
    }

    @Override
    public Price price(Room room, NewReservation request, PriceCalculator calculator) {
        return calculator.seasonalPrice(room, request.checkIn(), request.checkOut(), DISCOUNT);
    }

    @Override
    public BigDecimal refund(BigDecimal amountPaid) {
        return PriceCalculator.cents(BigDecimal.ZERO);
    }
}
