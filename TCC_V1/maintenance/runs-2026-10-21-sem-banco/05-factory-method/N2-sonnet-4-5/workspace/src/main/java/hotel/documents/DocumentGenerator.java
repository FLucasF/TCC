package hotel.documents;

import hotel.reservations.Reservation;
import hotel.rooms.Room;

/** Strategy for generating reservation documents based on guest type. */
public interface DocumentGenerator {

    ReservationReceipt generate(Reservation reservation, Room room);
}
