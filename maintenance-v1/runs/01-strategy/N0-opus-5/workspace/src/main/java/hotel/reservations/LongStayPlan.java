package hotel.reservations;

import java.math.BigDecimal;

/**
 * The long stay: 10% off the nights (high season included) for stays of seven nights or more.
 * Cancelling gives everything back.
 */
final class LongStayPlan extends NightlyDiscountPlan {

    static final int MINIMUM_NIGHTS = 7;
    static final BigDecimal DISCOUNT = new BigDecimal("0.10");

    @Override
    public void checkEligibility(Stay stay) {
        if (stay.nights() < MINIMUM_NIGHTS) {
            throw RatePlanPolicy.notEligible(
                    "the long stay rate needs at least " + MINIMUM_NIGHTS + " nights");
        }
    }

    @Override
    BigDecimal discountRate(Stay stay) {
        return DISCOUNT;
    }
}
