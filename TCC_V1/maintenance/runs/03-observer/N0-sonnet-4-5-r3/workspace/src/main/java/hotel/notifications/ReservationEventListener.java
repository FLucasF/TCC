package hotel.notifications;

import hotel.reservations.Reservation;
import hotel.rooms.Room;

/** Listens to reservation lifecycle events and reacts to them. */
public interface ReservationEventListener {

    /** Called when a reservation is confirmed (the guest has paid). */
    void onReservationConfirmed(Reservation reservation, Room room);

    /** Called when a confirmed reservation is cancelled. */
    void onReservationCancelled(Reservation reservation, Room room);
}
