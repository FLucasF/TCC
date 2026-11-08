package hotel.notifications;

import hotel.reservations.Reservation;
import hotel.rooms.Room;

/** Notified when something happens to a reservation. */
public interface ReservationListener {

    void onConfirmed(Reservation reservation, Room room);

    void onCancelled(Reservation reservation, Room room);
}
