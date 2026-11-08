package hotel.reservations;

import hotel.rooms.Room;

public record ReservationConfirmedEvent(Reservation reservation, Room room) {
}
