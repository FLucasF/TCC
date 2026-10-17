package hotel.reservations.rateplans;

import hotel.reservations.NewReservation;
import hotel.reservations.Price;
import hotel.reservations.PriceCalculator;
import hotel.rooms.Room;
import java.math.BigDecimal;

final class NonRefundablePolicy implements RatePlanPolicy {

    private static final BigDecimal DISCOUNT = new BigDecimal("0.15");

    @Override
    public Price price(NewReservation request, Room room, PriceCalculator pricing) {
        return pricing.nightlyPlanPrice(room, request.checkIn(), request.checkOut(), DISCOUNT);
    }

    @Override
    public BigDecimal refund(BigDecimal amountPaid, Price price) {
        return PriceCalculator.cents(BigDecimal.ZERO);
    }
}
