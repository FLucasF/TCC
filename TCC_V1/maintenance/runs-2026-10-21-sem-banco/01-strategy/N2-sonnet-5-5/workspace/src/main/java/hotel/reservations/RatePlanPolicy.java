package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;

/** The rules of one rate plan: who may use it, how it prices a stay and what comes back on cancellation. */
interface RatePlanPolicy {

    /** Refuses, with INVALID_REQUEST, a reservation this plan cannot be used for. */
    default void validate(NewReservation request) {
    }

    Price price(Room room, NewReservation request, PriceCalculator calculator);

    /** What comes back when a reservation that has paid {@code amountPaid} is cancelled. */
    BigDecimal refund(BigDecimal amountPaid);
}
