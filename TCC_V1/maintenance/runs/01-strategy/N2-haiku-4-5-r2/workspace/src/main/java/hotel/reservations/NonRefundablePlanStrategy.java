package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;

final class NonRefundablePlanStrategy implements RatePlanStrategy {

    private final PriceCalculator priceCalculator;

    NonRefundablePlanStrategy(PriceCalculator priceCalculator) {
        this.priceCalculator = priceCalculator;
    }

    @Override
    public void validate(NewReservation request) {
        // Non-refundable plan has no special validations
    }

    @Override
    public Price calculatePrice(Room room, NewReservation request) {
        return priceCalculator.nightlyPrice(room, request.checkIn(), request.checkOut(), PriceCalculator.NON_REFUNDABLE_DISCOUNT);
    }

    @Override
    public BigDecimal calculateRefund(BigDecimal amountPaid) {
        return PriceCalculator.cents(BigDecimal.ZERO);
    }
}
