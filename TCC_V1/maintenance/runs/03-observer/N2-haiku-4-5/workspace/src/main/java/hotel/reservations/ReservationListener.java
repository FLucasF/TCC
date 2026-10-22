package hotel.reservations;

import hotel.rooms.Room;

public interface ReservationListener {
    void onConfirmation(Reservation reservation, Room room);

    void onCancellation(Reservation reservation, Room room);
}
