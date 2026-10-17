package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;
import java.time.LocalDate;

/** What a rate plan decides: who can use it, what a stay costs, and what comes back when it is cancelled. */
interface RatePlanRules {

    default void checkEligibility(Room room, LocalDate checkIn, LocalDate checkOut, String companyCode) {
    }

    Price price(Room room, LocalDate checkIn, LocalDate checkOut, String companyCode);

    default BigDecimal refund(BigDecimal amountPaid) {
        return amountPaid;
    }
}
