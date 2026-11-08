package hotel.reservations;

/** Reacts to events that happen to reservations. */
public interface ReservationEventListener {
    void onReservationConfirmed(ReservationConfirmedEvent event);

    void onReservationCancelled(ReservationCancelledEvent event);
}
