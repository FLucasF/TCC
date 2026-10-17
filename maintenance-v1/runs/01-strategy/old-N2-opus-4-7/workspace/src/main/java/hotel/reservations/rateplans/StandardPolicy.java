package hotel.reservations.rateplans;

import hotel.reservations.NewReservation;
import hotel.reservations.Price;
import hotel.reservations.PriceCalculator;
import hotel.rooms.Room;
import java.math.BigDecimal;

final class StandardPolicy implements RatePlanPolicy {

    @Override
    public Price price(NewReservation request, Room room, PriceCalculator pricing) {
        return pricing.nightlyPlanPrice(room, request.checkIn(), request.checkOut(), BigDecimal.ZERO);
    }
}
