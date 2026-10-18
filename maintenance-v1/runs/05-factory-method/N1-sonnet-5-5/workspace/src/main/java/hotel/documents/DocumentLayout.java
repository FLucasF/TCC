package hotel.documents;

import hotel.reservations.Guest;
import hotel.reservations.Identification;
import hotel.reservations.Reservation;
import hotel.rooms.Room;

/** One kind of document: its title and how it reads. Each kind of guest identification has its own. */
interface DocumentLayout {

    String title();

    String text(Reservation reservation, Room room);

    static DocumentLayout forGuest(Guest guest) {
        return switch (guest.identification()) {
            case Identification.Cpf cpf -> new PortugueseReceiptLayout();
            case Identification.Company company -> new LodgingInvoiceLayout();
            case Identification.Foreigner foreigner -> new BookingReceiptLayout();
        };
    }
}
