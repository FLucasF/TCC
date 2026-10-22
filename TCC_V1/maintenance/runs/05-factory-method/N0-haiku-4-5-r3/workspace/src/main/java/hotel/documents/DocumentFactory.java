package hotel.documents;

import hotel.reservations.Reservation;
import hotel.rooms.Room;

/** Factory for creating the appropriate document type based on the reservation. */
public final class DocumentFactory {

    private DocumentFactory() {}

    /** Returns a document object (ReservationReceipt, CompanyInvoice, or InternationalBookingReceipt). */
    public static DocumentType createDocument(Reservation reservation, Room room) {
        if (reservation.guest().companyCnpj() != null) {
            return new CompanyInvoice(reservation, room);
        } else if (reservation.guest().guestPassport() != null) {
            return new InternationalBookingReceipt(reservation, room);
        } else {
            return new ReservationReceipt(reservation, room);
        }
    }
}
