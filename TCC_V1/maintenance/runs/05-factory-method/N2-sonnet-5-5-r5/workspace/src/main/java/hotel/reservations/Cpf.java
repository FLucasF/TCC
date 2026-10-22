package hotel.reservations;

import hotel.documents.DocumentLayout;
import hotel.documents.ReceiptLayout;
import java.util.Optional;

/** A Brazilian guest, identified by CPF. */
public record Cpf(String number) implements Identification {

    static Optional<Identification> from(IdentificationFields fields) {
        return IdentificationFields.isPresent(fields.cpf()) ? Optional.of(new Cpf(fields.cpf())) : Optional.empty();
    }

    @Override
    public DocumentLayout documentLayout() {
        return new ReceiptLayout();
    }

    @Override
    public String cpf() {
        return number;
    }
}
