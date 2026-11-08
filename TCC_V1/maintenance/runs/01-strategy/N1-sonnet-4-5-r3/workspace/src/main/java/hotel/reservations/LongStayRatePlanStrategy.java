package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** Long stay: 7 nights or more with 10% discount. Full refund on cancellation. */
final class LongStayRatePlanStrategy implements RatePlanStrategy {

    private static final BigDecimal DISCOUNT_RATE = new BigDecimal("0.10");
    private static final int MINIMUM_NIGHTS = 7;

    private final PriceCalculator priceCalculator;

    LongStayRatePlanStrategy(PriceCalculator priceCalculator) {
        this.priceCalculator = priceCalculator;
    }

    @Override
    public void validate(NewReservation request, Room room) {
        long nights = ChronoUnit.DAYS.between(request.checkIn(), request.checkOut());
        if (nights < MINIMUM_NIGHTS) {
            throw new HotelException(
                    ErrorCode.INVALID_REQUEST, "long stay requires at least " + MINIMUM_NIGHTS + " nights");
        }
    }

    @Override
    public Price calculatePrice(Room room, LocalDate checkIn, LocalDate checkOut) {
        return priceCalculator.calculateByNightlyRate(room, checkIn, checkOut, DISCOUNT_RATE);
    }

    @Override
    public BigDecimal refundAmount(BigDecimal amountPaid) {
        return amountPaid;
    }
}
