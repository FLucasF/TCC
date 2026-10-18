package hotel.notifications;

import hotel.reservations.Reservation;
import hotel.reservations.ReservationStatus;
import hotel.rooms.Room;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public final class ReservationNotifications {

    private final List<ReservationNotifier> notifiers;

    public ReservationNotifications(List<ReservationNotifier> notifiers) {
        this.notifiers = new ArrayList<>(notifiers);
    }

    public void notifyConfirmed(Reservation reservation, Room room) {
        for (ReservationNotifier notifier : notifiers) {
            notifier.onConfirmed(reservation, room);
        }
    }

    public void notifyCancelled(Reservation reservation, Room room, BigDecimal refund,
            ReservationStatus previousStatus) {
        if (previousStatus == ReservationStatus.CONFIRMED) {
            for (ReservationNotifier notifier : notifiers) {
                notifier.onCancelled(reservation, room, refund);
            }
        }
    }
}
