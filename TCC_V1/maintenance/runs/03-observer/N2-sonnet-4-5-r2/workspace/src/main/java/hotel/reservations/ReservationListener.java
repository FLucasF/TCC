package hotel.reservations;

import hotel.rooms.Room;

/** Reacts to reservation events (confirmed, cancelled). */
public interface ReservationListener {

    void onReservationConfirmed(Reservation reservation, Room room);

    void onReservationCancelled(Reservation reservation, Room room, boolean wasPaid);
}
