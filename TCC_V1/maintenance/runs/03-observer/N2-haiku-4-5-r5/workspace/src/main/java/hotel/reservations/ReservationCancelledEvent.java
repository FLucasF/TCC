package hotel.reservations;

import hotel.rooms.Room;

public record ReservationCancelledEvent(Reservation reservation, Room room) {
}
