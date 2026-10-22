package hotel.reservations;

import hotel.documents.DocumentLayout;

/**
 * How a guest is identified when booking. Each kind knows which document it gets; the fields that do not
 * apply to it are null.
 */
public interface Identification {

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
}
