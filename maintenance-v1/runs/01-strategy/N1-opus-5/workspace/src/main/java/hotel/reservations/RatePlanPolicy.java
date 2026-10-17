package hotel.reservations;

import java.math.BigDecimal;

/**
 * What a rate plan does: who may book it, what the stay costs, and how much comes back when the
 * guest cancels. Every plan answers these three questions in its own way.
 */
public interface RatePlanPolicy {

    /** Refuses the stay when the plan's rules do not allow it. Most plans take any stay. */
    default void checkBookable(NewReservation request) {
    }

    /** What the stay costs under this plan. */
    Price price(Stay stay);

    /** What the guest gets back on cancellation, out of what was paid. */
    BigDecimal refund(BigDecimal amountPaid);
}
