package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * The rules of a rate plan: how to check whether it can be used, how to calculate the price, and
 * what comes back when the reservation is cancelled.
 */
public interface RatePlanStrategy {

    /**
     * Checks whether this rate plan can be used for the given reservation. Throws HotelException
     * with INVALID_REQUEST if the plan's rules forbid it.
     */
    void validate(Room room, LocalDate checkIn, LocalDate checkOut, String companyCode);

    /** Calculates the price for this rate plan. */
    Price calculatePrice(Room room, LocalDate checkIn, LocalDate checkOut, String companyCode);

    /** Calculates what the guest gets back when cancelling a confirmed reservation. */
    BigDecimal calculateRefund(BigDecimal amountPaid);
}
