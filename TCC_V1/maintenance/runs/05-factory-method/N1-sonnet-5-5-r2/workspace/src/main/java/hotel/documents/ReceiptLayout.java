package hotel.documents;

import hotel.reservations.Reservation;
import java.util.List;

/** The receipt of the guest who has a CPF. */
final class ReceiptLayout extends DocumentLayout {

    @Override
    String title() {
        return "Comprovante de reserva";
    }

    @Override
    Language language() {
        return Language.PORTUGUESE;
    }

    @Override
    List<String> identityLines(Reservation reservation) {
        return List.of("Hóspede: " + reservation.guest().name()
                + " (CPF " + reservation.guest().identification().cpf() + ")");
    }
}
