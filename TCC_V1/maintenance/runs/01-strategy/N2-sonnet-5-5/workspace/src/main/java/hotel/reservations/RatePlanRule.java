package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;

/** What makes a rate plan different: who can use it, how the stay is priced and what comes back on cancelling. */
interface RatePlanRule {

    /**
     * The price of the stay under this plan, or INVALID_REQUEST if the request cannot use the plan.
     *
     * @param seasonal the stay priced night by night, with no discount
     */
    Price price(Room room, NewReservation request, Price seasonal);

    /** What goes back to the guest when cancelling after having paid {@code amountPaid}. */
    BigDecimal refund(BigDecimal amountPaid);
}
