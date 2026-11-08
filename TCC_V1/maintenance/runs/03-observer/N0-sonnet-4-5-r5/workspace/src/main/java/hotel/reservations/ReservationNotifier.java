package hotel.reservations;

import java.util.ArrayList;
import java.util.List;

public final class ReservationNotifier {

    private final List<ReservationEventListener> listeners = new ArrayList<>();

    public void addListener(ReservationEventListener listener) {
        listeners.add(listener);
    }

    public void notifyConfirmed(Reservation reservation, int roomNumber) {
        for (ReservationEventListener listener : listeners) {
            listener.onReservationConfirmed(reservation, roomNumber);
        }
    }

    public void notifyCancelled(Reservation reservation, int roomNumber, boolean wasConfirmed) {
        for (ReservationEventListener listener : listeners) {
            listener.onReservationCancelled(reservation, roomNumber, wasConfirmed);
        }
    }
}
