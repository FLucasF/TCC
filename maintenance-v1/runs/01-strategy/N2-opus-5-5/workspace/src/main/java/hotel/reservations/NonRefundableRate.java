package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;

/** 15% off, but nothing comes back on cancellation. */
final class NonRefundableRate implements RatePlanPolicy {

    static final BigDecimal DISCOUNT = new BigDecimal("0.15");

    @Override
    public Price price(Room room, NewReservation request, PriceCalculator calculator) {
        return calculator.nightlyRates(room, request.checkIn(), request.checkOut()).withDiscount(DISCOUNT);
    }

    @Override
    public BigDecimal refund(Reservation reservation) {
        return PriceCalculator.cents(BigDecimal.ZERO);
    }
}
