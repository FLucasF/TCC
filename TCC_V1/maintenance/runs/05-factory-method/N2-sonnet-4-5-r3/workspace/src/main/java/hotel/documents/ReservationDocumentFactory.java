package hotel.documents;

import hotel.reservations.CompanyGuest;
import hotel.reservations.ForeignGuest;
import hotel.reservations.PersonGuest;
import hotel.reservations.Reservation;
import hotel.rooms.Room;

/** Creates the appropriate document type based on the guest's identification. */
final class ReservationDocumentFactory {

    private ReservationDocumentFactory() {
    }

    static ReservationDocument create(Reservation reservation, Room room) {
        return switch (reservation.guest()) {
            case PersonGuest ignored -> new GuestReceipt(reservation, room);
            case CompanyGuest ignored -> new CompanyInvoice(reservation, room);
            case ForeignGuest ignored -> new BookingReceipt(reservation, room);
        };
    }
}
