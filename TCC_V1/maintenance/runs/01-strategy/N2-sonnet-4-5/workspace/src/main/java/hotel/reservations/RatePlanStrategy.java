package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A rate plan's rules: how to validate a stay, how to calculate the price, and what comes back when
 * the guest cancels.
 */
interface RatePlanStrategy {

    /** Checks that the stay is valid for this rate plan, throwing if it is not. */
    void validate(Room room, LocalDate checkIn, LocalDate checkOut, int nights, String companyCode);

    /** The price for this stay. */
    Price calculatePrice(Room room, LocalDate checkIn, LocalDate checkOut, int nights, String companyCode);

    /** What the guest gets back when cancelling. */
    BigDecimal calculateRefund(BigDecimal amountPaid);
}
