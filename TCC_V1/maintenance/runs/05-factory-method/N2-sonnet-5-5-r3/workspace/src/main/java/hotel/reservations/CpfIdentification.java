package hotel.reservations;

import hotel.documents.DocumentLayout;
import hotel.documents.ReceiptLayout;

/** A guest with a CPF. */
public record CpfIdentification(String cpf) implements Identification {

    static final String TYPE = "CPF";

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public String number() {
        return cpf;
    }

    @Override
    public String detail() {
        return null;
    }

    @Override
    public DocumentLayout layout() {
        return new ReceiptLayout();
    }
}
