package hotel.reservations;

import hotel.rooms.Room;

public interface ReservationListener {

    void onReservationConfirmed(Reservation reservation, Room room);

    void onReservationCancelled(Reservation reservation, Room room, boolean wasConfirmed);
}
