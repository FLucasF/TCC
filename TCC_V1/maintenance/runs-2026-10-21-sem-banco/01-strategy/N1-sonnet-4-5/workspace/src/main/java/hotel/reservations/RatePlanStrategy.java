package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * How a rate plan works: the price it gives, who can use it, and what comes back on cancellation.
 * Each plan is a class, so adding one is writing a class, not changing switches in several places.
 */
interface RatePlanStrategy {

    /** Checks whether this reservation is allowed under the plan's rules; throws if not. */
    void validate(Room room, LocalDate checkIn, LocalDate checkOut, String companyCode);

    /** The price of this stay under this plan. */
    Price calculate(Room room, LocalDate checkIn, LocalDate checkOut, String companyCode);

    /** What the guest gets back when cancelling a confirmed reservation. */
    BigDecimal refundAmount(BigDecimal amountPaid);
}
