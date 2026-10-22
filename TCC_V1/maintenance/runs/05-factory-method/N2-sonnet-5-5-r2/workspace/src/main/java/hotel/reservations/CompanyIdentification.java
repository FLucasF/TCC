package hotel.reservations;

import hotel.documents.DocumentLayout;
import hotel.documents.InvoiceLayout;
import java.util.Optional;

/** A company that books the room for an employee, and gets an invoice. */
public record CompanyIdentification(String cnpj, String name) implements Identification {

    private static final DocumentLayout LAYOUT = new InvoiceLayout();

    static Optional<Identification> from(IdentificationFields fields) {
        return Identifications.ifAny(
                new CompanyIdentification(fields.companyCnpj(), fields.companyName()),
                fields.companyCnpj(), fields.companyName());
    }

    @Override
    public IdentificationFields fields() {
        return new IdentificationFields(null, cnpj, name, null, null);
    }

    @Override
    public DocumentLayout documentLayout() {
        return LAYOUT;
    }
}
