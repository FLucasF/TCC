package hotel.documents;

import hotel.reservations.Guest;

/** The receipt of a guest with a CPF. */
public final class ReceiptLayout extends PortugueseLayout {

    @Override
    protected String title() {
        return "Comprovante de reserva";
    }

    @Override
    protected String identification(Guest guest) {
        return "Hóspede: " + guest.name() + " (CPF " + guest.identification().cpf() + ")\n";
    }
}
