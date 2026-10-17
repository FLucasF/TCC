package hotel.reservations;

import java.math.BigDecimal;

/** For stays of 7 nights or more: 10% off. */
final class LongStayPolicy extends FullRefundPolicy {

    static final int MINIMUM_NIGHTS = 7;
    static final BigDecimal DISCOUNT = new BigDecimal("0.10");

    @Override
    public Price price(Stay stay, Price fullPrice) {
        if (fullPrice.nights() < MINIMUM_NIGHTS) {
            throw new HotelException(
                    ErrorCode.INVALID_REQUEST, "the long stay plan needs at least " + MINIMUM_NIGHTS + " nights");
        }
        return fullPrice.withDiscountRate(DISCOUNT);
    }
}
