package hotel.reservations;

import java.math.BigDecimal;

/** Full price; the guest gets everything back when cancelling. */
final class StandardPlan extends DiscountedPlan {

    @Override
    protected BigDecimal discountRate(NewReservation request, Price regularPrice) {
        return BigDecimal.ZERO;
    }
}
