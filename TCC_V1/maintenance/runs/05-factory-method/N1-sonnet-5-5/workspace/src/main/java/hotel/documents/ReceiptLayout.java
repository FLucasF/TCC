package hotel.documents;

import hotel.reservations.Reservation;

/** The receipt of the guest who has a CPF. */
final class ReceiptLayout extends DocumentLayout {

    ReceiptLayout() {
        super(PORTUGUESE);
    }

    @Override
    String title() {
        return "Comprovante de reserva";
    }

    @Override
    String guestLines(Reservation reservation) {
        return "Hóspede: " + reservation.guest().name() + " (CPF " + reservation.guest().identification().cpf()
                + ")\n";
    }
}
