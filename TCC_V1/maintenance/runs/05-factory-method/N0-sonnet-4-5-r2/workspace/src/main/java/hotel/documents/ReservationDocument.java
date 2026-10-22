package hotel.documents;

import hotel.reservations.Reservation;
import hotel.rooms.Room;
import hotel.rooms.RoomType;
import java.math.BigDecimal;
import java.time.LocalDate;

/** A document about a reservation: receipt, invoice, or booking receipt. */
public sealed interface ReservationDocument permits PersonReceipt, CompanyInvoice, ForeignerReceipt {

    String title();
    long reservationId();
    String guestName();
    String guestCpf();
    String companyCnpj();
    String companyName();
    String guestPassport();
    String guestCountry();
    int roomNumber();
    RoomType roomType();
    LocalDate checkIn();
    LocalDate checkOut();
    int nights();
    BigDecimal subtotal();
    BigDecimal discount();
    BigDecimal total();

    /** The formatted text of the document. */
    String text();

    /** Creates the appropriate document type for the given reservation. */
    static ReservationDocument of(Reservation reservation, Room room) {
        if (reservation.guest().isCompany()) {
            return new CompanyInvoice(reservation, room);
        } else if (reservation.guest().isForeigner()) {
            return new ForeignerReceipt(reservation, room);
        } else {
            return new PersonReceipt(reservation, room);
        }
    }
}
