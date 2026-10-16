package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;

final class NonRefundablePolicy implements RatePlanPolicy {

    private final PriceCalculator priceCalculator;

    NonRefundablePolicy(PriceCalculator priceCalculator) {
        this.priceCalculator = priceCalculator;
    }

    @Override
    public void validate(NewReservation request, Room room) {
    }

    @Override
    public Price calculate(Room room, NewReservation request) {
        return priceCalculator.calculate(room, request.checkIn(), request.checkOut(), RatePlan.NON_REFUNDABLE);
    }

    @Override
    public BigDecimal refund(Reservation reservation) {
        return PriceCalculator.cents(BigDecimal.ZERO);
    }
}
