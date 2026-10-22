package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;

/**
 * What a rate plan decides: who may use it and for which stay (rejecting with INVALID_REQUEST), how the
 * stay is priced, and how much of what was paid comes back when the guest cancels. A new plan is a new
 * policy, registered in {@link RatePlan}.
 */
interface RatePlanPolicy {

    /** Prices the stay, or throws a {@link HotelException} when the plan cannot be used for it. */
    Price price(PriceCalculator calculator, Room room, NewReservation request);

    /** What goes back to the guest, given what they paid. */
    BigDecimal refund(BigDecimal amountPaid);
}
