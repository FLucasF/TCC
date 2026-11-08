package hotel.notifications;

import hotel.reservations.Reservation;
import hotel.rooms.Room;

public interface Notifier {
    void onReservationConfirmed(Reservation reservation, Room room);

    void onReservationCancelled(Reservation reservation, Room room);
}
