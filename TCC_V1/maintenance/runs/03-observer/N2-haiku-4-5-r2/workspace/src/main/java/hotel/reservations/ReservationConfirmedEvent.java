package hotel.reservations;

import hotel.rooms.Room;

/** A reservation has been confirmed (guest paid). */
public record ReservationConfirmedEvent(Reservation reservation, Room room) implements ReservationEvent {}
