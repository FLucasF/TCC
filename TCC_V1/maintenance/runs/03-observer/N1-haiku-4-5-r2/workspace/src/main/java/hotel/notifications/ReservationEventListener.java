package hotel.notifications;

import hotel.reservations.Reservation;
import hotel.rooms.Room;

public interface ReservationEventListener {

    void onReservationConfirmed(Reservation reservation, Room room);

    void onReservationCancelled(Reservation reservation, Room room);
}
