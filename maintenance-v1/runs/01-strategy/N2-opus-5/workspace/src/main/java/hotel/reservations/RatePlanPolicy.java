package hotel.reservations;

import java.math.BigDecimal;

/**
 * What a rate plan does: it says who may use it, it prices the stay its own way, and it says how much
 * of what was paid comes back when the guest cancels. There is one of these for every {@link RatePlan},
 * so a new plan is a new class and a new constant, and nothing else changes.
 */
public interface RatePlanPolicy {

    /**
     * Refuses the stay with {@link ErrorCode#INVALID_REQUEST} when it cannot be booked under this plan.
     * Plans open to any stay say nothing.
     */
    default void validate(Stay stay) {
    }

    Price price(Stay stay);

    /** What comes back to the guest, out of what they paid, when the reservation is cancelled. */
    BigDecimal refund(BigDecimal amountPaid);
}
