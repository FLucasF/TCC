package hotel.reservations;

import hotel.rooms.Room;

/** Notified when something happens to a reservation. */
public interface ReservationEventListener {

    /** A reservation was confirmed: the guest paid. */
    void onReservationConfirmed(Reservation reservation, Room room);

    /** A confirmed reservation was cancelled. */
    void onReservationCancelled(Reservation reservation, Room room);
}
