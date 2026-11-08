package hotel.notifications;

import hotel.reservations.Reservation;

/** Event fired when a reservation is confirmed (guest paid). */
public record ReservationConfirmed(Reservation reservation) implements ReservationEvent {
}
