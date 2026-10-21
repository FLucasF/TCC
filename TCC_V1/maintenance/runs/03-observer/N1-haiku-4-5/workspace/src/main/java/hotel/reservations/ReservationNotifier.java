package hotel.reservations;

public interface ReservationNotifier {
    void onConfirmation(Reservation reservation);
    void onCancellation(Reservation reservation);
}
