package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;

/** 7 nights or more, 10% off; everything paid comes back on cancellation. */
final class LongStayPlan implements RatePlanRules {

    static final int MINIMUM_NIGHTS = 7;
    static final BigDecimal DISCOUNT = new BigDecimal("0.10");

    @Override
    public void checkEligibility(NewReservation request) {
        if (ChronoUnit.DAYS.between(request.checkIn(), request.checkOut()) < MINIMUM_NIGHTS) {
            throw new HotelException(
                    ErrorCode.INVALID_REQUEST, "the long stay rate needs at least " + MINIMUM_NIGHTS + " nights");
        }
    }

    @Override
    public Price price(Room room, NewReservation request, PriceCalculator calculator) {
        return calculator.seasonalPrice(room, request.checkIn(), request.checkOut(), DISCOUNT);
    }

    @Override
    public BigDecimal refund(BigDecimal amountPaid) {
        return amountPaid;
    }
}
