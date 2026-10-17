package hotel.reservations;

import java.math.BigDecimal;

/** Full price, and everything comes back on cancellation. */
final class StandardPlan extends DiscountedNightsPlan {

    @Override
    BigDecimal discountRate(Stay stay) {
        return BigDecimal.ZERO;
    }

    @Override
    public BigDecimal refund(BigDecimal amountPaid) {
        return amountPaid;
    }
}
