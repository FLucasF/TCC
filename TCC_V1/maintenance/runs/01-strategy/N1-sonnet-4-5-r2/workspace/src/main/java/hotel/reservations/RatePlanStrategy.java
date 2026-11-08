package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A rate plan's behavior: how it validates a stay, calculates the price, and refunds on cancellation.
 * Each plan knows its own rules; adding a new plan means implementing this interface.
 */
interface RatePlanStrategy {

    /** Checks whether this plan allows the given stay. Throws HotelException if not. */
    void validate(NewReservation request, Room room);

    /** Calculates the discount for this plan, given the subtotal of the nightly rates. */
    BigDecimal calculateDiscount(BigDecimal subtotal, NewReservation request, Room room, LocalDate checkIn, LocalDate checkOut);

    /** How much the guest gets back when cancelling a confirmed reservation. */
    BigDecimal refundAmount(BigDecimal amountPaid);
}
