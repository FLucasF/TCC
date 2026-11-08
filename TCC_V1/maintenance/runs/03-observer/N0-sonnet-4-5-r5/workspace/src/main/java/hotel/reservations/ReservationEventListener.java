package hotel.reservations;

public interface ReservationEventListener {

    void onReservationConfirmed(Reservation reservation, int roomNumber);

    void onReservationCancelled(Reservation reservation, int roomNumber, boolean wasConfirmed);
}
