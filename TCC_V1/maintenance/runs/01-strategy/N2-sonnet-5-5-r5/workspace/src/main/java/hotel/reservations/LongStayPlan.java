package hotel.reservations;

import java.math.BigDecimal;

/** 10% off for stays of 7 nights or more. */
final class LongStayPlan extends DiscountedPlan {

    static final int MINIMUM_NIGHTS = 7;
    static final BigDecimal DISCOUNT = new BigDecimal("0.10");

    @Override
    protected BigDecimal discountRate(NewReservation request, Price regularPrice) {
        if (regularPrice.nights() < MINIMUM_NIGHTS) {
            throw new HotelException(
                    ErrorCode.INVALID_REQUEST, "the long stay rate needs at least " + MINIMUM_NIGHTS + " nights");
        }
        return DISCOUNT;
    }
}
