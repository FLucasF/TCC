package hotel.reservations;

import hotel.rooms.Room;

/** Observes reservation state changes and reacts to them. */
public interface ReservationObserver {

    /** Called when a reservation is confirmed (the guest paid). */
    void onConfirmation(Reservation reservation, Room room);

    /** Called when a confirmed reservation is cancelled. */
    void onCancellation(Reservation reservation, Room room);
}
