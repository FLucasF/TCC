package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;

final class LongStayPlanStrategy implements RatePlanStrategy {

    private static final BigDecimal LONG_STAY_DISCOUNT = new BigDecimal("0.10");

    private final PriceCalculator priceCalculator;

    LongStayPlanStrategy(PriceCalculator priceCalculator) {
        this.priceCalculator = priceCalculator;
    }

    @Override
    public void validate(NewReservation request) {
        long nights = java.time.temporal.ChronoUnit.DAYS.between(request.checkIn(), request.checkOut());
        if (nights < 7) {
            throw new HotelException(
                    ErrorCode.INVALID_REQUEST,
                    "long stay rate requires at least 7 nights");
        }
    }

    @Override
    public Price calculatePrice(Room room, NewReservation request) {
        return priceCalculator.nightlyPrice(room, request.checkIn(), request.checkOut(), LONG_STAY_DISCOUNT);
    }

    @Override
    public BigDecimal calculateRefund(BigDecimal amountPaid) {
        return amountPaid;
    }
}
