package hotel.notifications;

import hotel.reservations.Reservation;
import hotel.rooms.Room;
import java.math.BigDecimal;

public interface ReservationNotifier {

    void onConfirmed(Reservation reservation, Room room);

    void onCancelled(Reservation reservation, Room room, BigDecimal refund);
}
