package hotel.documents;

import hotel.reservations.Guest;
import hotel.reservations.Price;
import hotel.rooms.Room;
import java.time.LocalDate;

/** Generates a reservation document in the format appropriate for the guest type. */
public interface DocumentGenerator {

    /** The document's title. */
    String title();

    /** The formatted text of the document. */
    String formatDocument(
            long reservationId,
            Guest guest,
            Room room,
            LocalDate checkIn,
            LocalDate checkOut,
            Price price);
}
