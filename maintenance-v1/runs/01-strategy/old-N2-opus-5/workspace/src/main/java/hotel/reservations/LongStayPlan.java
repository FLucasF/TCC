package hotel.reservations;

import java.math.BigDecimal;

/** Seven nights or more: 10% off the nights, high season included. Everything comes back. */
final class LongStayPlan extends DiscountedNightsPlan {

    private static final int MINIMUM_NIGHTS = 7;
    private static final BigDecimal DISCOUNT = new BigDecimal("0.10");

    @Override
    public void checkEligibility(Stay stay) {
        if (stay.nights() < MINIMUM_NIGHTS) {
            throw new HotelException(
                    ErrorCode.INVALID_REQUEST,
                    "the long stay plan needs at least " + MINIMUM_NIGHTS + " nights");
        }
    }

    @Override
    BigDecimal discountRate(Stay stay) {
        return DISCOUNT;
    }

    @Override
    public BigDecimal refund(BigDecimal amountPaid) {
        return amountPaid;
    }
}
