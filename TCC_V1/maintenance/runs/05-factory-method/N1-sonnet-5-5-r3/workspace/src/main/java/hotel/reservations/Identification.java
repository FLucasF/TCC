package hotel.reservations;

/**
 * How the guest is identified on a reservation: by CPF, by the company paying (CNPJ and company name) or by
 * passport. The accessors of what does not apply to the kind of identification return null.
 */
public sealed interface Identification {

    record Cpf(String number) implements Identification {
        @Override
        public String cpf() {
            return number;
        }
    }

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

    record Passport(String number, String country) implements Identification {
        @Override
        public String guestPassport() {
            return number;
        }

        @Override
        public String guestCountry() {
            return country;
        }
    }

    default String cpf() {
        return null;
    }

    default String companyCnpj() {
        return null;
    }

    default String companyName() {
        return null;
    }

    default String guestPassport() {
        return null;
    }

    default String guestCountry() {
        return null;
    }

    /** Builds the identification from the fields as they arrive; exactly one complete kind must be filled in. */
    static Identification of(String cpf, String companyCnpj, String companyName, String passport, String country) {
        boolean byCpf = !isBlank(cpf);
        boolean byCompany = !isBlank(companyCnpj) || !isBlank(companyName);
        boolean byPassport = !isBlank(passport) || !isBlank(country);
        if (byCpf && !byCompany && !byPassport) {
            return new Cpf(cpf);
        }
        if (byCompany && !byCpf && !byPassport && !isBlank(companyCnpj) && !isBlank(companyName)) {
            return new Company(companyCnpj, companyName);
        }
        if (byPassport && !byCpf && !byCompany && !isBlank(passport) && !isBlank(country)) {
            return new Passport(passport, country);
        }
        throw new HotelException(
                ErrorCode.INVALID_REQUEST,
                "identify the guest by guestCpf, or by companyCnpj and companyName, or by guestPassport and guestCountry");
    }

    private static boolean isBlank(String text) {
        return text == null || text.isBlank();
    }
}
