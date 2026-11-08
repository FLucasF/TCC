package hotel.notifications;

import hotel.reservations.Reservation;
import hotel.reservations.ReservationStatus;

/** Event fired when a reservation is cancelled. */
public record ReservationCancelled(Reservation reservation, boolean wasConfirmed) implements ReservationEvent {

    public ReservationCancelled(Reservation reservation, ReservationStatus previousStatus) {
        this(reservation, previousStatus == ReservationStatus.CONFIRMED);
    }
}
