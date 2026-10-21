package hotel.reservations;

/** Reacts to reservation confirmation and cancellation events. */
public interface ReservationEventListener {

    void onReservationConfirmed(Reservation reservation);

    void onReservationCancelled(Reservation reservation);
}
