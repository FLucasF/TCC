package hotel.reservations;

import hotel.rooms.Room;

/** Reacts to events in the life of a reservation. */
public interface ReservationEventListener {

    /** A pending reservation was confirmed: the guest paid. */
    void onConfirmed(Reservation reservation, Room room);

    /** A confirmed reservation was cancelled: the room became available and money may be refunded. */
    void onCancelled(Reservation reservation, Room room);
}
