package hotel.notifications;

import hotel.reservations.Reservation;
import hotel.rooms.Room;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public final class ReservationEventNotifier {

    private final List<ReservationEventListener> listeners = new ArrayList<>();

    public void addListener(ReservationEventListener listener) {
        listeners.add(listener);
    }

    public void notifyConfirmed(Reservation reservation, Room room) {
        for (ReservationEventListener listener : listeners) {
            listener.onReservationConfirmed(reservation, room);
        }
    }

    public void notifyCancelled(Reservation reservation, Room room, BigDecimal refund) {
        for (ReservationEventListener listener : listeners) {
            listener.onReservationCancelled(reservation, room, refund);
        }
    }
}
