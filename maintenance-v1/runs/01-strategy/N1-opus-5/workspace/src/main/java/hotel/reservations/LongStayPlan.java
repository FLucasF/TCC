package hotel.reservations;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;

/**
 * The long stay: from seven nights on, 10% off the nights (the high-season surcharge still holds on
 * the high-season ones). Everything comes back on cancellation.
 */
final class LongStayPlan implements RatePlanPolicy {

    static final int MINIMUM_NIGHTS = 7;
    private static final BigDecimal DISCOUNT = new BigDecimal("0.10");

    @Override
    public void checkBookable(NewReservation request) {
        long nights = ChronoUnit.DAYS.between(request.checkIn(), request.checkOut());
        if (nights < MINIMUM_NIGHTS) {
            throw new HotelException(
                    ErrorCode.INVALID_REQUEST,
                    "the long stay plan needs " + MINIMUM_NIGHTS + " nights or more");
        }
    }

    @Override
    public Price price(Stay stay) {
        return Price.percentOff(stay.nights(), stay.nightsSubtotal(), DISCOUNT);
    }

    @Override
    public BigDecimal refund(BigDecimal amountPaid) {
        return amountPaid;
    }
}
