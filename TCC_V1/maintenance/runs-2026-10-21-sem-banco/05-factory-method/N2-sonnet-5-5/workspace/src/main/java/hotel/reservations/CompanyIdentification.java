package hotel.reservations;

import hotel.documents.DocumentLayout;
import hotel.documents.InvoiceLayout;

/** A guest staying on behalf of a company. */
public record CompanyIdentification(String companyCnpj, String companyName) implements Identification {

    @Override
    public DocumentLayout documentLayout() {
        return new InvoiceLayout();
    }
}
