package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;

/** The rules of one rate plan: how the stay is priced, who may book it, and what comes back on cancellation. */
interface RatePlanPolicy {

    /**
     * The price of the stay under this plan. {@code regularPrice} is the stay at the room's nightly rates, with
     * the high-season surcharge and no discount. Throws INVALID_REQUEST when the plan does not allow the request.
     */
    Price price(Room room, NewReservation request, Price regularPrice);

    /** What goes back to the guest when the reservation is cancelled. */
    BigDecimal refund(Reservation reservation);
}
