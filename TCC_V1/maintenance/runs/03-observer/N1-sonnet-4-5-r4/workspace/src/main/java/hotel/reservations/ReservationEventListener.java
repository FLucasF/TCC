package hotel.reservations;

import hotel.rooms.Room;

/** Receives notifications when something happens to a reservation. */
public interface ReservationEventListener {

    /** A pending reservation was paid and confirmed. */
    void onConfirmed(Reservation reservation, Room room);

    /** A reservation was cancelled. */
    void onCancelled(Reservation reservation, Room room);
}
