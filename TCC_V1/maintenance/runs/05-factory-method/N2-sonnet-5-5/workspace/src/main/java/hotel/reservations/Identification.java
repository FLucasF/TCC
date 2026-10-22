package hotel.reservations;

import hotel.documents.DocumentLayout;

/**
 * How the guest of a reservation is identified. Each kind brings the document it gets; the fields that do
 * not apply to a kind are null.
 */
public interface Identification {

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
