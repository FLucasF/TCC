package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;

/**
 * How a rate plan behaves: who can use it, what the stay costs, and how much comes back when the
 * guest cancels. New plans drop in by implementing this interface and registering with
 * {@link RatePlanPolicies}.
 */
public interface RatePlanPolicy {

    /** Rejects the reservation (with {@link ErrorCode#INVALID_REQUEST}) when it does not fit the plan. */
    void validate(NewReservation request, Room room);

    /** The price of the stay under this plan. */
    Price calculate(Room room, NewReservation request);

    /** What comes back when a reservation on this plan is cancelled. */
    BigDecimal refund(Reservation reservation);
}
