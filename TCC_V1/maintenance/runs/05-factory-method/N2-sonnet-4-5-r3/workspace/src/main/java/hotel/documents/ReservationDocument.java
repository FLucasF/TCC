package hotel.documents;

import hotel.reservations.Reservation;
import hotel.rooms.Room;
import java.math.BigDecimal;
import java.time.LocalDate;

/** A document issued for a reservation: receipt, invoice or booking confirmation. */
public interface ReservationDocument {
    String title();
    long reservationId();
    int roomNumber();
    String roomType();
    LocalDate checkIn();
    LocalDate checkOut();
    int nights();
    BigDecimal subtotal();
    BigDecimal discount();
    BigDecimal total();

    /** Guest identification fields (null for fields that don't apply to this document type). */
    String guestName();
    String guestCpf();
    String companyCnpj();
    String companyName();
    String guestPassport();
    String guestCountry();

    /** The complete text of the document as it appears in emails. */
    String text();

    static ReservationDocument create(Reservation reservation, Room room) {
        return ReservationDocumentFactory.create(reservation, room);
    }
}
