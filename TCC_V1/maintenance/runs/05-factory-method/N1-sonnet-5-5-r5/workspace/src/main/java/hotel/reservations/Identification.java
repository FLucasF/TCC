package hotel.reservations;

/**
 * How the guest of a reservation is identified. Each kind says which of the identification fields it fills;
 * the others are null.
 */
public sealed interface Identification {

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

    /** A Brazilian person. */
    record Cpf(String number) implements Identification {
        @Override
        public String cpf() {
            return number;
        }
    }

    /** A company booking a room for the guest. */
    record Company(String cnpj, String name) implements Identification {
        @Override
        public String companyCnpj() {
            return cnpj;
        }

        @Override
        public String companyName() {
            return name;
        }
    }

    /** A foreigner. */
    record Passport(String number, String issuingCountry) implements Identification {
        @Override
        public String passport() {
            return number;
        }

        @Override
        public String country() {
            return issuingCountry;
        }
    }
}
