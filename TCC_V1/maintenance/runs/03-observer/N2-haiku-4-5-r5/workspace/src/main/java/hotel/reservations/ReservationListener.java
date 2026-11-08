package hotel.reservations;

public interface ReservationListener {
    void onReservationConfirmed(ReservationConfirmedEvent event);

    void onReservationCancelled(ReservationCancelledEvent event);
}
