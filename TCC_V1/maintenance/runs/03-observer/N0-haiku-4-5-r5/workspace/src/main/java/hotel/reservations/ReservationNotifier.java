package hotel.reservations;

/** Notifies interested parties about reservation events. */
public interface ReservationNotifier {
    void onReservationConfirmed(Reservation reservation);

    void onReservationCancelled(Reservation reservation);
}
