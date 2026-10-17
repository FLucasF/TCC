package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;

/** What a rate plan decides: who may use it, what the stay costs, and what comes back on cancellation. */
interface RatePlanPolicy {

    /** Throws INVALID_REQUEST when the reservation does not follow the plan's rules. */
    default void checkEligibility(NewReservation request) {
    }

    Price price(Room room, NewReservation request, PriceCalculator calculator);

    /** What the guest gets back on cancellation (a pending reservation was never paid). */
    BigDecimal refund(Reservation reservation);
}
