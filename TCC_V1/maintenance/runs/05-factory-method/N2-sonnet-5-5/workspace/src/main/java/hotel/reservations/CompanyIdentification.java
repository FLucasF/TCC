package hotel.reservations;

import hotel.documents.DocumentLayout;
import hotel.documents.InvoiceLayout;

/** A guest staying on behalf of a company, which is invoiced. */
public record CompanyIdentification(String companyCnpj, String companyName) implements Identification {

    @Override
    public DocumentLayout layout() {
        return new InvoiceLayout();
    }
}
