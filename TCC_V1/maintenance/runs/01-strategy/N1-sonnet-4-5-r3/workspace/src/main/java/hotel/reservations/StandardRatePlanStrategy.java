package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Full price; the guest gets everything back when cancelling a confirmed reservation. */
final class StandardRatePlanStrategy implements RatePlanStrategy {

    private final PriceCalculator priceCalculator;

    StandardRatePlanStrategy(PriceCalculator priceCalculator) {
        this.priceCalculator = priceCalculator;
    }

    @Override
    public void validate(NewReservation request, Room room) {
        // no restrictions
    }

    @Override
    public Price calculatePrice(Room room, LocalDate checkIn, LocalDate checkOut) {
        return priceCalculator.calculateByNightlyRate(room, checkIn, checkOut, BigDecimal.ZERO);
    }

    @Override
    public BigDecimal refundAmount(BigDecimal amountPaid) {
        return amountPaid;
    }
}
