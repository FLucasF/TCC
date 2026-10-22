package hotel.documents;

import hotel.reservations.Reservation;
import hotel.rooms.Room;
import hotel.rooms.RoomType;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Base interface for all reservation documents (receipt, invoice, booking receipt). */
public interface ReservationDocument {

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

    String text();

    static ReservationDocument create(Reservation reservation, Room room) {
        return switch (reservation.guest().type()) {
            case INDIVIDUAL -> new ReservationReceipt(reservation, room);
            case COMPANY -> new InvoiceDocument(reservation, room);
            case FOREIGNER -> new BookingReceiptDocument(reservation, room);
        };
    }
}
