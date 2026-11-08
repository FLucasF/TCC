package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;

final class StandardPlanStrategy implements RatePlanStrategy {

    private final PriceCalculator priceCalculator;

    StandardPlanStrategy(PriceCalculator priceCalculator) {
        this.priceCalculator = priceCalculator;
    }

    @Override
    public void validate(NewReservation request) {
        // Standard plan has no special validations
    }

    @Override
    public Price calculatePrice(Room room, NewReservation request) {
        return priceCalculator.nightlyPrice(room, request.checkIn(), request.checkOut(), BigDecimal.ZERO);
    }

    @Override
    public BigDecimal calculateRefund(BigDecimal amountPaid) {
        return amountPaid;
    }
}
