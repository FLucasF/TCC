package hotel.reservations;

/** An event that happened to a reservation. */
public sealed interface ReservationEvent permits ReservationConfirmedEvent, ReservationCancelledEvent {
    Reservation reservation();
}
