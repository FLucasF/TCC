package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;

/** 10% off for stays of 7 nights or more; everything paid comes back on cancellation. */
final class LongStayRate implements RatePlanPolicy {

    static final BigDecimal DISCOUNT = new BigDecimal("0.10");
    static final int MINIMUM_NIGHTS = 7;

    @Override
    public void checkEligibility(NewReservation request) {
        if (ChronoUnit.DAYS.between(request.checkIn(), request.checkOut()) < MINIMUM_NIGHTS) {
            throw new HotelException(
                    ErrorCode.INVALID_REQUEST,
                    "the LONG_STAY rate plan needs at least " + MINIMUM_NIGHTS + " nights");
        }
    }

    @Override
    public Price price(Room room, NewReservation request, PriceCalculator calculator) {
        return calculator.nightlyRates(room, request.checkIn(), request.checkOut()).withDiscount(DISCOUNT);
    }

    @Override
    public BigDecimal refund(Reservation reservation) {
        return reservation.amountPaid();
    }
}
