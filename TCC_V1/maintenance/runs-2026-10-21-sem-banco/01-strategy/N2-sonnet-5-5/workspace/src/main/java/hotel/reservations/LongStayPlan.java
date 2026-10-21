package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;

/** 10% off for stays of 7 nights or more; everything comes back on cancellation. */
final class LongStayPlan implements RatePlanPolicy {

    static final BigDecimal DISCOUNT = new BigDecimal("0.10");
    static final int MINIMUM_NIGHTS = 7;

    @Override
    public void validate(NewReservation request) {
        if (ChronoUnit.DAYS.between(request.checkIn(), request.checkOut()) < MINIMUM_NIGHTS) {
            throw new HotelException(
                    ErrorCode.INVALID_REQUEST, "the LONG_STAY plan needs at least " + MINIMUM_NIGHTS + " nights");
        }
    }

    @Override
    public Price price(Room room, NewReservation request, PriceCalculator calculator) {
        return calculator.discounted(room, request, DISCOUNT);
    }

    @Override
    public BigDecimal refund(BigDecimal amountPaid) {
        return amountPaid;
    }
}
