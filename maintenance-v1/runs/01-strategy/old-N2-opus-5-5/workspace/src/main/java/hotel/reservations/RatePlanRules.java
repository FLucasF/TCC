package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;

/** How a rate plan works: who may use it, what the stay costs, and what comes back on cancellation. */
interface RatePlanRules {

    /** Refuses, with INVALID_REQUEST, a reservation this plan cannot be used for. */
    void checkEligibility(NewReservation request);

    Price price(Room room, NewReservation request, PriceCalculator calculator);

    /** What the guest gets back when cancelling, out of what they paid (zero if they never paid). */
    BigDecimal refund(BigDecimal amountPaid);
}
