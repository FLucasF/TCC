package hotel.notifications;

import hotel.reservations.Reservation;
import hotel.rooms.Room;
import java.math.BigDecimal;

public interface ReservationEventListener {

    void onReservationConfirmed(Reservation reservation, Room room);

    void onReservationCancelled(Reservation reservation, Room room, BigDecimal refund);
}
