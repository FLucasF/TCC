package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;

/** Full price, for anyone; everything paid comes back on cancellation. */
final class StandardPlan implements RatePlanRules {

    @Override
    public void checkEligibility(NewReservation request) {
    }

    @Override
    public Price price(Room room, NewReservation request, PriceCalculator calculator) {
        return calculator.seasonalPrice(room, request.checkIn(), request.checkOut(), BigDecimal.ZERO);
    }

    @Override
    public BigDecimal refund(BigDecimal amountPaid) {
        return amountPaid;
    }
}
