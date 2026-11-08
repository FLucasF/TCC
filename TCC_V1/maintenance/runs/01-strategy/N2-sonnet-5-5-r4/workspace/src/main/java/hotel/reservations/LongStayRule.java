package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Seven nights or more, with 10% off; everything comes back on cancellation. */
final class LongStayRule implements RatePlanRule {

    static final int MINIMUM_NIGHTS = 7;
    static final BigDecimal DISCOUNT = new BigDecimal("0.10");

    @Override
    public Price price(Room room, LocalDate checkIn, LocalDate checkOut, String companyCode, Price standardPrice) {
        if (standardPrice.nights() < MINIMUM_NIGHTS) {
            throw new HotelException(
                    ErrorCode.INVALID_REQUEST, "the long stay plan needs at least " + MINIMUM_NIGHTS + " nights");
        }
        return RatePlanRule.withDiscount(standardPrice, DISCOUNT);
    }

    @Override
    public BigDecimal refund(BigDecimal amountPaid) {
        return amountPaid;
    }
}
