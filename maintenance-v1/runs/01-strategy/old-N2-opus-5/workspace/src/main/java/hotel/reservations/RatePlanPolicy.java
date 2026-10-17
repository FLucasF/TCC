package hotel.reservations;

import java.math.BigDecimal;

/**
 * A rate plan: its way of pricing a stay, its rules about who can book it, and what comes back to the
 * guest when the reservation is cancelled. A new plan is one more implementation of this.
 */
interface RatePlanPolicy {

    /** Refuses, with INVALID_REQUEST, a stay that does not fit the plan's rules. */
    default void checkEligibility(Stay stay) {
    }

    Price price(Stay stay);

    /** What the guest gets back, out of what was paid, when cancelling before the check-in. */
    BigDecimal refund(BigDecimal amountPaid);
}
