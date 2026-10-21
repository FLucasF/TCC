package hotel.documents;

import hotel.reservations.Guest;
import hotel.reservations.GuestType;
import java.util.Map;

/** Selects the appropriate document generator based on guest type. */
public final class DocumentGenerators {

    private static final Map<GuestType, DocumentGenerator> GENERATORS = Map.of(
            GuestType.INDIVIDUAL, new IndividualReceiptGenerator(),
            GuestType.COMPANY, new CompanyInvoiceGenerator(),
            GuestType.FOREIGNER, new ForeignerReceiptGenerator());

    private DocumentGenerators() {
    }

    public static DocumentGenerator forGuest(Guest guest) {
        return GENERATORS.get(guest.type());
    }
}
