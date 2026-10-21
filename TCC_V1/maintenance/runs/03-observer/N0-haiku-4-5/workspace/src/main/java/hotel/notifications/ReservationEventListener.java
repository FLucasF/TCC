package hotel.notifications;

import hotel.reservations.Reservation;
import hotel.rooms.Room;

/** Listens to reservation events to send notifications to departments. */
public interface ReservationEventListener {

    void onReservationConfirmed(Reservation reservation, Room room);

    void onReservationCancelled(Reservation reservation, Room room);
}
