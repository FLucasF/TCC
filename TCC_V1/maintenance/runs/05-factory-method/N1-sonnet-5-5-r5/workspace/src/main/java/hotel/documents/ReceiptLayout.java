package hotel.documents;

import hotel.reservations.Reservation;

/** The receipt of a guest with a CPF. */
final class ReceiptLayout extends DocumentLayout {

    ReceiptLayout() {
        super("Comprovante de reserva", DocumentLanguage.PORTUGUESE);
    }

    @Override
    String guestLines(Reservation reservation) {
        return guestName(reservation) + " (CPF " + reservation.guest().identification().cpf() + ")\n";
    }
}
