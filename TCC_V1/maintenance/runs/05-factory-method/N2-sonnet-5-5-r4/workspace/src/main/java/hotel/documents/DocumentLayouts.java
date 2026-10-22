package hotel.documents;

import hotel.reservations.CompanyIdentification;
import hotel.reservations.CpfIdentification;
import hotel.reservations.Identification;
import hotel.reservations.PassportIdentification;
import java.util.Map;

/** The one place that says which document each kind of identification gets. */
final class DocumentLayouts {

    private static final Map<Class<? extends Identification>, DocumentLayout> LAYOUTS = Map.of(
            CpfIdentification.class, new ReceiptLayout(),
            CompanyIdentification.class, new InvoiceLayout(),
            PassportIdentification.class, new BookingReceiptLayout());

    private DocumentLayouts() {
    }

    static DocumentLayout of(Identification identification) {
        return LAYOUTS.get(identification.getClass());
    }
}
