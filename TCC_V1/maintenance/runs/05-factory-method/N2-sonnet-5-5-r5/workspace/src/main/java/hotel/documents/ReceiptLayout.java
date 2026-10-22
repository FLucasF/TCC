package hotel.documents;

import hotel.reservations.Reservation;
import java.util.List;

/** The receipt for a guest with a CPF. */
public final class ReceiptLayout extends DocumentLayout {

    public ReceiptLayout() {
        super(new PortugueseWording());
    }

    @Override
    String title() {
        return "Comprovante de reserva";
    }

    @Override
    List<String> partyLines(Reservation reservation) {
        return List.of(guestLine(reservation, "CPF " + reservation.guest().identification().cpf()));
    }
}
