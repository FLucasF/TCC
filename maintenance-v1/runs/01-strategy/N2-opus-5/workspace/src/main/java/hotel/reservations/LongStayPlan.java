package hotel.reservations;

import java.math.BigDecimal;

/**
 * The long stay: seven nights or more get 10% off the nights, high season still counting on the
 * high-season ones. Everything paid comes back on cancellation.
 */
public final class LongStayPlan implements RatePlanPolicy {

    static final int MINIMUM_NIGHTS = 7;
    static final BigDecimal DISCOUNT = new BigDecimal("0.10");

    @Override
    public void validate(Stay stay) {
        if (stay.nights() < MINIMUM_NIGHTS) {
            throw new HotelException(
                    ErrorCode.INVALID_REQUEST,
                    "the long stay plan needs at least " + MINIMUM_NIGHTS + " nights");
        }
    }

    @Override
    public Price price(Stay stay) {
        return PriceCalculator.nightsWithDiscount(stay, DISCOUNT);
    }

    @Override
    public BigDecimal refund(BigDecimal amountPaid) {
        return amountPaid;
    }
}
