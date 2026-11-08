package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * The pricing and refund rules for a rate plan. Each plan validates the booking request, calculates
 * the price, and determines what the guest gets back on cancellation.
 */
interface RatePlanStrategy {

    /**
     * Validates whether this rate plan can be used for the given reservation.
     *
     * @throws HotelException with INVALID_REQUEST if the plan's rules are not met
     */
    void validate(Room room, LocalDate checkIn, LocalDate checkOut, String companyCode);

    /** Calculates the price for this reservation. */
    Price calculatePrice(Room room, LocalDate checkIn, LocalDate checkOut, String companyCode);

    /** What the guest gets back when cancelling a confirmed reservation. */
    BigDecimal calculateRefund(BigDecimal amountPaid);
}
