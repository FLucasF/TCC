package hotel.documents;

import hotel.reservations.Reservation;
import hotel.rooms.Room;

public final class DocumentFactory {

    private DocumentFactory() {
    }

    public static ReservationReceipt create(Reservation reservation, Room room) {
        if (reservation.guest().cpf() != null) {
            return new CpfReceipt(reservation, room);
        } else if (reservation.guest().companyCnpj() != null) {
            return new CnpjInvoice(reservation, room);
        } else if (reservation.guest().guestPassport() != null) {
            return new PassportReceipt(reservation, room);
        } else {
            throw new IllegalStateException("Reservation has no valid identification");
        }
    }
}
