package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;

/** A plan that gives a percentage off the sum of the nights. Cancelling gives back everything paid. */
abstract class DiscountedPlan implements RatePlanPolicy {

    /** The fraction taken off the regular price; throws INVALID_REQUEST when the plan does not allow the request. */
    protected abstract BigDecimal discountRate(NewReservation request, Price regularPrice);

    @Override
    public final Price price(Room room, NewReservation request, Price regularPrice) {
        BigDecimal discount = PriceCalculator.cents(regularPrice.subtotal().multiply(discountRate(request, regularPrice)));
        return new Price(
                regularPrice.nights(), regularPrice.subtotal(), discount, regularPrice.subtotal().subtract(discount));
    }

    @Override
    public BigDecimal refund(Reservation reservation) {
        return reservation.amountPaid();
    }
}
