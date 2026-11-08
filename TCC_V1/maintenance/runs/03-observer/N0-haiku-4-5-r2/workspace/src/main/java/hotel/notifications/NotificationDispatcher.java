package hotel.notifications;

import hotel.reservations.Reservation;
import hotel.rooms.Room;
import java.util.ArrayList;
import java.util.List;

public final class NotificationDispatcher {

    private final List<Notifier> notifiers = new ArrayList<>();

    public void register(Notifier notifier) {
        notifiers.add(notifier);
    }

    public void notifyReservationConfirmed(Reservation reservation, Room room) {
        for (Notifier notifier : notifiers) {
            notifier.onReservationConfirmed(reservation, room);
        }
    }

    public void notifyReservationCancelled(Reservation reservation, Room room) {
        for (Notifier notifier : notifiers) {
            notifier.onReservationCancelled(reservation, room);
        }
    }
}
