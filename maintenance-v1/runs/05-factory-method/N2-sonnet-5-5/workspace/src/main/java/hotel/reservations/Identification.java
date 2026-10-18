package hotel.reservations;

/**
 * How the guest is identified on the reservation, which decides the document they get. The accessors of
 * the data that does not apply to a kind of identification answer null.
 */
public sealed interface Identification {

    record Cpf(String cpf) implements Identification {
    }

    record Company(String cnpj, String companyName) implements Identification {
    }

    record Passport(String passport, String country) implements Identification {
    }

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
}
