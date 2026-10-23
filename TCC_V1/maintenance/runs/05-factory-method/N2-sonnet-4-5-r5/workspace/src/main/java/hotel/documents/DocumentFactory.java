package hotel.documents;

import hotel.reservations.Reservation;
import hotel.rooms.Room;

/** Creates the appropriate document type based on the guest type. */
public final class DocumentFactory {

    private DocumentFactory() {
    }

    public static ReservationDocument create(Reservation reservation, Room room) {
        return switch (reservation.guest().type()) {
            case INDIVIDUAL -> new IndividualReceipt(reservation, room);
            case COMPANY -> new CompanyInvoice(reservation, room);
            case FOREIGN -> new BookingReceipt(reservation, room);
        };
    }
}
