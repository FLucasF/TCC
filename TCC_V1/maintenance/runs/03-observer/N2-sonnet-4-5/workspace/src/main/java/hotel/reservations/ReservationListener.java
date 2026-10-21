package hotel.reservations;

import hotel.rooms.Room;

/** Listens to reservation events: confirmed or cancelled. */
public interface ReservationListener {

    /** A reservation was confirmed: the guest paid. */
    void onReservationConfirmed(Reservation reservation, Room room);

    /** A reservation was cancelled. */
    void onReservationCancelled(Reservation reservation, Room room);
}
