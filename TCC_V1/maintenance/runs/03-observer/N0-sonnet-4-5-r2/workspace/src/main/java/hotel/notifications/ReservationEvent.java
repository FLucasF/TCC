package hotel.notifications;

import hotel.reservations.Reservation;

/** Base interface for reservation events. */
public sealed interface ReservationEvent permits ReservationConfirmed, ReservationCancelled {
    Reservation reservation();
}
