package hotel.reservations;

/**
 * How a guest is identified on a reservation. Each kind exposes only its own fields; the others are null.
 */
public sealed interface Identification {

    default String cpf() {
        return null;
    }

    default String cnpj() {
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

    /** A Brazilian individual. */
    record Cpf(String cpf) implements Identification {
    }

    /** A company booking a room for an employee. */
    record Company(String cnpj, String companyName) implements Identification {
    }

    /** A foreign guest. */
    record Passport(String passport, String country) implements Identification {
    }
}
