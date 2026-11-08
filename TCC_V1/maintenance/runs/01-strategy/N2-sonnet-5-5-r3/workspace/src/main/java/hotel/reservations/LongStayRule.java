package hotel.reservations;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;

final class LongStayRule extends DiscountedRule {

    static final int MINIMUM_NIGHTS = 7;
    static final BigDecimal DISCOUNT = new BigDecimal("0.10");

    @Override
    protected BigDecimal discountRate(NewReservation request) {
        long nights = ChronoUnit.DAYS.between(request.checkIn(), request.checkOut());
        if (nights < MINIMUM_NIGHTS) {
            throw new HotelException(
                    ErrorCode.INVALID_REQUEST, "the long stay rate needs at least " + MINIMUM_NIGHTS + " nights");
        }
        return DISCOUNT;
    }

    @Override
    public BigDecimal refund(Reservation reservation) {
        return reservation.amountPaid();
    }
}
