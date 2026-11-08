package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * The rules of a rate plan: how to validate a reservation, how to calculate its price, and how much
 * the guest gets back when they cancel.
 */
interface RatePlanStrategy {

    /** Throws INVALID_REQUEST if the reservation cannot be made with this plan. */
    void validate(NewReservation request, Room room);

    /** The price for this stay. */
    Price calculatePrice(Room room, LocalDate checkIn, LocalDate checkOut);

    /** What the guest gets back when cancelling a confirmed reservation (that has been paid). */
    BigDecimal refundAmount(BigDecimal amountPaid);
}
