package hotel.reservations;

import hotel.rooms.Room;

/** Reacts to reservation events: confirmation and cancellation. */
public interface ReservationListener {

    /** Called when a reservation is confirmed (the guest paid). */
    void onConfirmed(Reservation reservation, Room room);

    /** Called when a confirmed reservation is cancelled. */
    void onCancelled(Reservation reservation, Room room);
}
