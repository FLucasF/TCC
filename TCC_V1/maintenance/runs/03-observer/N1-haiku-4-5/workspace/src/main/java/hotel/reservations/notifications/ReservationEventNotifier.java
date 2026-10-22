package hotel.reservations.notifications;

import hotel.reservations.Reservation;
import hotel.rooms.Room;

public interface ReservationEventNotifier {
    void notifyConfirmation(Reservation reservation, Room room);

    void notifyCancellation(Reservation reservation, Room room);
}
