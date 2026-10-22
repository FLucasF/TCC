package hotel.reservations;

import hotel.documents.DocumentLayout;

/**
 * How a guest is identified on a reservation. Each kind knows how it is saved, which fields it shows to the website
 * and which document the guest gets.
 */
public interface Identification {

    /** The kind, as saved in the database. */
    String type();

    /** The main value (CPF, CNPJ, passport number), as saved in the database. */
    String number();

    /** The second value (company name, country), as saved in the database; null when the kind has none. */
    String detail();

    /** The document this kind of guest gets. */
    DocumentLayout layout();

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
}
