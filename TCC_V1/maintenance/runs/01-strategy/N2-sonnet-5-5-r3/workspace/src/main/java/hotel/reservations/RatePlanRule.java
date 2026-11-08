package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;

/** What is particular to a rate plan: who may book it, what the stay costs and what comes back on cancelling. */
interface RatePlanRule {

    /**
     * The price of the stay under this plan.
     *
     * @param undiscounted the nights at the room's rates (high season included), with no discount
     * @throws HotelException INVALID_REQUEST when the request cannot use this plan
     */
    Price price(Room room, NewReservation request, Price undiscounted);

    /** What goes back to the guest when the reservation is cancelled. */
    BigDecimal refund(Reservation reservation);
}
