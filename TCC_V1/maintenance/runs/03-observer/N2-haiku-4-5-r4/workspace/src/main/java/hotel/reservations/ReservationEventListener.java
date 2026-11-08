package hotel.reservations;

public interface ReservationEventListener {

    void onReservationConfirmed(Reservation reservation);

    void onReservationCancelled(Reservation reservation);
}
