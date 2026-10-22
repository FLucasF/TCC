package hotel.documents;

import hotel.reservations.CorporateIdentity;
import hotel.reservations.ForeignIdentity;
import hotel.reservations.GuestIdentity;
import hotel.reservations.IndividualIdentity;

/** Chooses the right document generator for each guest identity type. */
public final class DocumentGenerators {

    private static final BrazilianReceiptGenerator BRAZILIAN_RECEIPT = new BrazilianReceiptGenerator();
    private static final CorporateInvoiceGenerator CORPORATE_INVOICE = new CorporateInvoiceGenerator();
    private static final EnglishReceiptGenerator ENGLISH_RECEIPT = new EnglishReceiptGenerator();

    private DocumentGenerators() {
    }

    public static DocumentGenerator forIdentity(GuestIdentity identity) {
        return switch (identity) {
            case IndividualIdentity i -> BRAZILIAN_RECEIPT;
            case CorporateIdentity c -> CORPORATE_INVOICE;
            case ForeignIdentity f -> ENGLISH_RECEIPT;
        };
    }
}
