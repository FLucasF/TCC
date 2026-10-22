package hotel.reservations;

import hotel.rooms.Room;

/** Listens to reservation events: confirmations, cancellations, and more. */
public interface ReservationListener {

    /** The guest paid and the reservation is confirmed. */
    void onConfirmation(Reservation reservation, Room room);

    /** The reservation was cancelled. */
    void onCancellation(Reservation reservation, Room room);
}
