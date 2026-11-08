package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;

/** A plan that takes a percentage off the sum of the nights. */
abstract class DiscountedRule implements RatePlanRule {

    @Override
    public Price price(Room room, NewReservation request, Price undiscounted) {
        BigDecimal discount = PriceCalculator.cents(undiscounted.subtotal().multiply(discountRate(request)));
        return new Price(
                undiscounted.nights(), undiscounted.subtotal(), discount, undiscounted.subtotal().subtract(discount));
    }

    /** The fraction taken off, e.g. 0.15; refuses the request when the plan does not apply to it. */
    protected abstract BigDecimal discountRate(NewReservation request);
}
