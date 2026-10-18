package hotel.documents;

import hotel.reservations.Identification;
import hotel.reservations.Reservation;
import hotel.rooms.Room;

/** Issues the right document for a reservation, according to how its guest is identified. */
public final class Documents {

    private static final DocumentLayout RECEIPT = new ReceiptLayout();
    private static final DocumentLayout INVOICE = new InvoiceLayout();
    private static final DocumentLayout BOOKING_RECEIPT = new BookingReceiptLayout();

    public ReservationDocument issue(Reservation reservation, Room room) {
        DocumentLayout layout = switch (reservation.guest().identification()) {
            case Identification.Cpf cpf -> RECEIPT;
            case Identification.Company company -> INVOICE;
            case Identification.Passport passport -> BOOKING_RECEIPT;
        };
        return ReservationDocument.issue(reservation, room, layout);
    }
}
