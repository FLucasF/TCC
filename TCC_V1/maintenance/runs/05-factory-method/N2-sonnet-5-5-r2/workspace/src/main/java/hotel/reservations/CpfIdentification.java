package hotel.reservations;

import hotel.documents.DocumentLayout;
import hotel.documents.ReceiptLayout;
import java.util.Optional;

/** A guest with CPF. */
public record CpfIdentification(String cpf) implements Identification {

    private static final DocumentLayout LAYOUT = new ReceiptLayout();

    static Optional<Identification> from(IdentificationFields fields) {
        return Identifications.ifAny(new CpfIdentification(fields.cpf()), fields.cpf());
    }

    @Override
    public IdentificationFields fields() {
        return new IdentificationFields(cpf, null, null, null, null);
    }

    @Override
    public DocumentLayout documentLayout() {
        return LAYOUT;
    }
}
