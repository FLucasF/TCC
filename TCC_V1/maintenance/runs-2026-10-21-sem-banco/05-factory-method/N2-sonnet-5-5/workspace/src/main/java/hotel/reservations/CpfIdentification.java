package hotel.reservations;

import hotel.documents.DocumentLayout;
import hotel.documents.ReceiptLayout;

/** A guest with a CPF. */
public record CpfIdentification(String cpf) implements Identification {

    @Override
    public DocumentLayout documentLayout() {
        return new ReceiptLayout();
    }
}
