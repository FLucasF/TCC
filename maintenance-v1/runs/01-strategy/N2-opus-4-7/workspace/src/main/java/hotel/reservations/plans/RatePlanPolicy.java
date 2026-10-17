package hotel.reservations.plans;

import hotel.reservations.NewReservation;
import hotel.reservations.Price;
import hotel.rooms.Room;
import java.math.BigDecimal;

/**
 * The rules of a rate plan: who can use it, how its price is computed, and what the guest gets back
 * when cancelling. Each plan is a policy; new plans arrive by adding a new one, not by editing the
 * old ones.
 */
public interface RatePlanPolicy {

    /** Rejects a request this plan does not accept, with {@code INVALID_REQUEST}. */
    void validate(NewReservation request);

    Price price(Room room, NewReservation request, PricingSupport pricing);

    /** What the guest gets back when cancelling a reservation they already paid for. */
    BigDecimal refund(BigDecimal amountPaid);
}
