package hotel.reservations;

import hotel.documents.DocumentLayout;
import hotel.documents.BookingReceiptLayout;
import hotel.documents.InvoiceLayout;
import hotel.documents.ReceiptLayout;

/**
 * How a reservation is identified, and so which document it gets. Each way answers null for the data
 * it does not have.
 */
public sealed interface Identification {

    /** The document this kind of reservation gets, when it is confirmed, at check-out and on request. */
    DocumentLayout documentLayout();

    default String cpf() {
        return null;
    }

    default String companyCnpj() {
        return null;
    }

    default String companyName() {
        return null;
    }

    default String passport() {
        return null;
    }

    default String country() {
        return null;
    }

    /** A Brazilian person: gets the receipt. */
    record Cpf(String cpf) implements Identification {
        @Override
        public DocumentLayout documentLayout() {
            return new ReceiptLayout();
        }
    }

    /** A company booking for the guest: gets the lodging invoice. */
    record Company(String companyCnpj, String companyName) implements Identification {
        @Override
        public DocumentLayout documentLayout() {
            return new InvoiceLayout();
        }
    }

    /** A foreigner: gets the booking receipt, in English. */
    record Passport(String passport, String country) implements Identification {
        @Override
        public DocumentLayout documentLayout() {
            return new BookingReceiptLayout();
        }
    }
}
