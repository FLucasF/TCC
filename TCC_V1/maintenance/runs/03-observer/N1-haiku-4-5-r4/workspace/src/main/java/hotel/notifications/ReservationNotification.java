package hotel.notifications;

import hotel.reservations.Reservation;
import hotel.rooms.Room;

/** Handles notifications when a reservation is confirmed or cancelled. */
public interface ReservationNotification {

    void onConfirmation(Reservation reservation, Room room);

    void onCancellation(Reservation reservation, Room room);
}
