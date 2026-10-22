package hotel.documents;

import com.fasterxml.jackson.annotation.JsonProperty;
import hotel.reservations.*;
import hotel.rooms.Room;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * The document a guest receives when the reservation is confirmed, at check-out, and on request.
 * The format varies by guest type: individuals get a receipt, companies get an invoice, foreigners get
 * a booking receipt in English.
 */
public sealed interface ReservationDocument permits IndividualReceipt, CorporateInvoice, ForeignReceipt {

    /** The formatted text of the document, ready to be sent or displayed. */
    @JsonProperty("text")
    String text();

    /** Shared document data for JSON responses. */
    long reservationId();
    String guestName();
    String guestCpf();
    String companyCnpj();
    String companyName();
    String guestPassport();
    String guestCountry();
    int roomNumber();
    LocalDate checkIn();
    LocalDate checkOut();
    int nights();
    BigDecimal subtotal();
    BigDecimal discount();
    BigDecimal total();

    /** Creates the appropriate document type for the guest. */
    static ReservationDocument from(Reservation reservation, Room room) {
        Guest guest = reservation.guest();
        return switch (guest) {
            case IndividualGuest individual -> new IndividualReceipt(reservation, room, individual);
            case CorporateGuest corporate -> new CorporateInvoice(reservation, room, corporate);
            case ForeignGuest foreign -> new ForeignReceipt(reservation, room, foreign);
        };
    }
}
