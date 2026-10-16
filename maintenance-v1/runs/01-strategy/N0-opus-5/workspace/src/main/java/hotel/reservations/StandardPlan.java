package hotel.reservations;

import java.math.BigDecimal;

/** Full price, open to everyone, and the guest gets everything back when cancelling. */
final class StandardPlan extends NightlyDiscountPlan {

    @Override
    BigDecimal discountRate(Stay stay) {
        return BigDecimal.ZERO;
    }
}
