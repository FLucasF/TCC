package hotel.documents;

import hotel.reservations.Identification;
import hotel.reservations.Reservation;

/** The receipt of a guest with a CPF. */
public final class ReceiptLayout extends PortugueseLayout {

    @Override
    public String title() {
        return "Comprovante de reserva";
    }

    @Override
    protected String guestLines(Reservation reservation) {
        Identification identification = reservation.guest().identification();
        return "Hóspede: " + reservation.guest().name() + " (CPF " + identification.cpf() + ")\n";
    }
}
