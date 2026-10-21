package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;

/**
 * The logic of one rate plan: validating whether a reservation can use it, calculating its price,
 * and deciding the refund on cancellation.
 */
public interface RatePlanStrategy {

    /** Throws HotelException if the reservation cannot use this plan. */
    void validate(NewReservation request, Room room);

    /** Calculates the price for this stay under this plan. */
    Price calculatePrice(NewReservation request, Room room);

    /** What the guest gets back when cancelling this reservation. */
    BigDecimal calculateRefund(Reservation reservation);
}
