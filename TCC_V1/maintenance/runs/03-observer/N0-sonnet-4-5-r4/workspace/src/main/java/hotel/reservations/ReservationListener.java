package hotel.reservations;

/**
 * Receives notifications when important reservation events happen. Departments can subscribe to be
 * notified about confirmations, cancellations, and other events they care about.
 */
public interface ReservationListener {

    /** A pending reservation was confirmed: the guest paid and the room is theirs. */
    void onReservationConfirmed(Reservation reservation);

    /** A confirmed reservation was cancelled: the room is now available again. */
    void onReservationCancelled(Reservation reservation);
}
