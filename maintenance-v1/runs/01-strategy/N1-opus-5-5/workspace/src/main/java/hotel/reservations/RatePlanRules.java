package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;

/** What makes a rate plan: who may use it, what the stay costs, and what comes back on cancellation. */
interface RatePlanRules {

    /** Refuses, with INVALID_REQUEST, a reservation that cannot use this plan. */
    default void checkEligibility(NewReservation request) {
    }

    /** The price of the stay, given what its nights cost with the season (and no discount). */
    Price price(NewReservation request, Room room, Price seasonalPrice);

    /** What comes back to the guest who paid {@code amountPaid} and cancels before the stay. */
    BigDecimal refund(BigDecimal amountPaid);
}
