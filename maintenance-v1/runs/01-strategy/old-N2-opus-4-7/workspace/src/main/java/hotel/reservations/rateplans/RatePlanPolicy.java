package hotel.reservations.rateplans;

import hotel.reservations.NewReservation;
import hotel.reservations.Price;
import hotel.reservations.PriceCalculator;
import hotel.rooms.Room;
import java.math.BigDecimal;

/**
 * One rate plan's rules in one place: who can book it, how its stay is priced, and how much comes
 * back when it is cancelled. Adding a new rate plan means adding one of these.
 */
public interface RatePlanPolicy {

    /** Rejects a stay the plan does not accept. The default accepts anything the service already allows. */
    default void validate(NewReservation request, Room room) {}

    /** Full price of the stay under this plan. */
    Price price(NewReservation request, Room room, PriceCalculator pricing);

    /** What the guest gets back when they cancel. Defaults to the whole paid amount. */
    default BigDecimal refund(BigDecimal amountPaid, Price price) {
        return amountPaid;
    }
}
