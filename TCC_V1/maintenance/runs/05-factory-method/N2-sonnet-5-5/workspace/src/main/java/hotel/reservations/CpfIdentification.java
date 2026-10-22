package hotel.reservations;

import hotel.documents.DocumentLayout;
import hotel.documents.ReceiptLayout;

/** A Brazilian guest, identified by CPF. */
public record CpfIdentification(String cpf) implements Identification {

    @Override
    public DocumentLayout layout() {
        return new ReceiptLayout();
    }
}
