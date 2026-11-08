package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;

/** What makes a rate plan itself: who may use it, how the stay is priced and what comes back on cancellation. */
interface RatePlanPolicy {

    /**
     * Prices the stay under this plan, or rejects the request when the plan does not allow it.
     *
     * @param base the stay at the room's nightly rates, with high season already applied and no discount
     */
    Price price(Room room, NewReservation request, Price base);

    /** What the guest gets back when cancelling, given what was paid. */
    BigDecimal refund(BigDecimal amountPaid);

    static Price discounted(Price base, BigDecimal percentage) {
        BigDecimal discount = PriceCalculator.cents(base.subtotal().multiply(percentage));
        return new Price(base.nights(), base.subtotal(), discount, base.subtotal().subtract(discount));
    }

    static BigDecimal percentOf(BigDecimal amount, BigDecimal percentage) {
        return PriceCalculator.cents(amount.multiply(percentage));
    }
}
