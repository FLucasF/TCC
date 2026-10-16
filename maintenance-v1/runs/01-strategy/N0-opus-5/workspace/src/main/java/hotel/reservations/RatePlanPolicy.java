package hotel.reservations;

import java.math.BigDecimal;

/**
 * What a rate plan knows: who may use it, what the stay costs on it, and how much of what the guest
 * paid comes back when the reservation is cancelled. A new plan is a new implementation of this
 * interface, wired into {@link RatePlan}.
 */
public interface RatePlanPolicy {

    /** Lets a stay the plan does not accept through as an {@code INVALID_REQUEST}. */
    default void checkEligibility(Stay stay) {
    }

    Price price(Stay stay);

    /** What the guest gets back on cancellation, out of the {@code amountPaid}. */
    BigDecimal refund(BigDecimal amountPaid);

    static HotelException notEligible(String reason) {
        return new HotelException(ErrorCode.INVALID_REQUEST, reason);
    }
}
