package hotel.notifications;

import hotel.reservations.Reservation;
import hotel.rooms.Room;

/** Notified when something happens to a reservation. */
public interface ReservationEventListener {

    /** The guest paid and the reservation was confirmed. */
    void onReservationConfirmed(Reservation reservation, Room room);

    /** A confirmed reservation was cancelled. Pending reservations are cancelled silently. */
    void onReservationCancelled(Reservation reservation, Room room);
}
