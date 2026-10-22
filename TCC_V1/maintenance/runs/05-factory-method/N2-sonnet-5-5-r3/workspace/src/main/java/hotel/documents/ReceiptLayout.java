package hotel.documents;

import hotel.reservations.Guest;

/** The receipt of a guest with a CPF. */
public final class ReceiptLayout extends DocumentLayout {

    public ReceiptLayout() {
        super("Comprovante de reserva", Wording.PORTUGUESE);
    }

    @Override
    String identification(Guest guest) {
        return "Hóspede: " + guest.name() + " (CPF " + guest.identification().cpf() + ")\n";
    }
}
