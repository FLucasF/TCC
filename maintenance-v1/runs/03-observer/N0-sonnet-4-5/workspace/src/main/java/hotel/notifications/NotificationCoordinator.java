package hotel.notifications;

import hotel.reservations.Reservation;
import hotel.reservations.ReservationStatus;
import hotel.rooms.Room;
import java.util.List;

public final class NotificationCoordinator {

    private final List<ReservationNotifier> notifiers;

    public NotificationCoordinator(List<ReservationNotifier> notifiers) {
        this.notifiers = List.copyOf(notifiers);
    }

    public void notifyConfirmed(Reservation reservation, Room room) {
        for (ReservationNotifier notifier : notifiers) {
            notifier.onConfirmed(reservation, room);
        }
    }

    public void notifyCancelled(Reservation reservation, Room room, boolean wasConfirmed) {
        if (wasConfirmed) {
            for (ReservationNotifier notifier : notifiers) {
                notifier.onCancelled(reservation, room);
            }
        }
    }
}
