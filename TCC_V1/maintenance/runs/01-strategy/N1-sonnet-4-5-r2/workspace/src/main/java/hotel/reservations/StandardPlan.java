package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;
import java.time.LocalDate;

final class StandardPlan implements RatePlanStrategy {

    @Override
    public void validate(NewReservation request, Room room) {
        // no restrictions
    }

    @Override
    public BigDecimal calculateDiscount(
            BigDecimal subtotal, NewReservation request, Room room, LocalDate checkIn, LocalDate checkOut) {
        return PriceCalculator.cents(BigDecimal.ZERO);
    }

    @Override
    public BigDecimal refundAmount(BigDecimal amountPaid) {
        return amountPaid;
    }
}
