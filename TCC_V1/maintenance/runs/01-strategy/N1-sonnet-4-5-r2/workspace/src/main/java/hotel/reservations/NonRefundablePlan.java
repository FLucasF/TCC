package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;
import java.time.LocalDate;

final class NonRefundablePlan implements RatePlanStrategy {

    static final BigDecimal DISCOUNT_RATE = new BigDecimal("0.15");

    @Override
    public void validate(NewReservation request, Room room) {
        // no restrictions
    }

    @Override
    public BigDecimal calculateDiscount(
            BigDecimal subtotal, NewReservation request, Room room, LocalDate checkIn, LocalDate checkOut) {
        return PriceCalculator.cents(subtotal.multiply(DISCOUNT_RATE));
    }

    @Override
    public BigDecimal refundAmount(BigDecimal amountPaid) {
        return PriceCalculator.cents(BigDecimal.ZERO);
    }
}
