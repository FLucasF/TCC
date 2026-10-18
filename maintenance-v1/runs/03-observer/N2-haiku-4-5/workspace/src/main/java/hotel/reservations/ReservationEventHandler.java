package hotel.reservations;

import hotel.rooms.Room;

/** Handles events when a reservation is confirmed or cancelled. */
public interface ReservationEventHandler {
    void onConfirmed(Reservation reservation, Room room);

    void onCancelled(Reservation reservation, Room room);
}
