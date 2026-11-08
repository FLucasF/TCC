package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;

/** Strategy for a specific rate plan: validation, price calculation, and refund rules. */
public interface RatePlanStrategy {
    /**
     * Validates whether a reservation request is valid for this plan. Throws HotelException if not.
     */
    void validate(NewReservation request);

    /**
     * Calculates the price for a stay according to this plan's rules.
     */
    Price calculatePrice(Room room, NewReservation request);

    /**
     * Calculates the refund amount when a confirmed reservation is cancelled.
     */
    BigDecimal calculateRefund(BigDecimal amountPaid);
}
