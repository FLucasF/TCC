package hotel.notifications;

import hotel.reservations.Reservation;
import hotel.rooms.Room;

/** Notifies stakeholders when reservation events occur. */
public interface ReservationNotifier {
    void onConfirmed(Reservation reservation, Room room);

    void onCancelled(Reservation reservation, Room room);
}
