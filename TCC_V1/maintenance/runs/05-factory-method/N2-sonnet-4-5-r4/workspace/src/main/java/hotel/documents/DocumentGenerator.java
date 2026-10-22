package hotel.documents;

import hotel.reservations.Reservation;
import hotel.rooms.Room;

/** Strategy for generating reservation documents. Each guest identity type has its own document format. */
public interface DocumentGenerator {
    ReservationReceipt generate(Reservation reservation, Room room);
}
