package hotel.reservations;

import hotel.documents.DocumentLayout;
import hotel.documents.InvoiceLayout;
import java.util.Optional;

/** A company booking a room for an employee, identified by CNPJ and company name. */
public record Company(String cnpj, String name) implements Identification {

    static Optional<Identification> from(IdentificationFields fields) {
        boolean hasCnpj = IdentificationFields.isPresent(fields.cnpj());
        boolean hasName = IdentificationFields.isPresent(fields.companyName());
        if (!hasCnpj && !hasName) {
            return Optional.empty();
        }
        if (!hasCnpj || !hasName) {
            throw new HotelException(ErrorCode.INVALID_REQUEST, "a company needs both companyCnpj and companyName");
        }
        return Optional.of(new Company(fields.cnpj(), fields.companyName()));
    }

    @Override
    public DocumentLayout documentLayout() {
        return new InvoiceLayout();
    }

    @Override
    public String companyCnpj() {
        return cnpj;
    }

    @Override
    public String companyName() {
        return name;
    }
}
