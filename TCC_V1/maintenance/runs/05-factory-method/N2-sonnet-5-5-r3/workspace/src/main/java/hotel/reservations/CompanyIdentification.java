package hotel.reservations;

import hotel.documents.DocumentLayout;
import hotel.documents.InvoiceLayout;

/** A guest who stays on behalf of a company, which is invoiced. */
public record CompanyIdentification(String companyCnpj, String companyName) implements Identification {

    static final String TYPE = "COMPANY";

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public String number() {
        return companyCnpj;
    }

    @Override
    public String detail() {
        return companyName;
    }

    @Override
    public DocumentLayout layout() {
        return new InvoiceLayout();
    }
}
