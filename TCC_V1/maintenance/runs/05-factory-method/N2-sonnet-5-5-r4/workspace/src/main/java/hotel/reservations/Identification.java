package hotel.reservations;

/**
 * How the person or company behind a reservation is identified. Each kind answers only for its own fields; the
 * others are null.
 */
public interface Identification {

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
