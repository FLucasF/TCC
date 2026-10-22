package hotel.reservations;

/**
 * How a guest is identified on the reservation: by CPF, by the company that pays (CNPJ and company name), or
 * by passport and country. The accessors of the kinds that do not apply are null.
 */
public sealed interface Identification {

    record Cpf(String cpf) implements Identification {
    }

    record Company(String companyCnpj, String companyName) implements Identification {
    }

    record Passport(String passportNumber, String passportCountry) implements Identification {
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

    default String passportNumber() {
        return null;
    }

    default String passportCountry() {
        return null;
    }

    /** Builds the identification from the flat fields; exactly one kind must be given, complete. */
    static Identification of(String cpf, String companyCnpj, String companyName, String passport, String country) {
        boolean hasCpf = filled(cpf);
        boolean hasCompany = filled(companyCnpj) || filled(companyName);
        boolean hasPassport = filled(passport) || filled(country);
        if ((hasCpf ? 1 : 0) + (hasCompany ? 1 : 0) + (hasPassport ? 1 : 0) != 1) {
            throw new HotelException(ErrorCode.INVALID_REQUEST,
                    "give either guestCpf, or companyCnpj with companyName, or guestPassport with guestCountry");
        }
        if (hasCpf) {
            return new Cpf(cpf);
        }
        if (hasCompany) {
            if (!filled(companyCnpj) || !filled(companyName)) {
                throw new HotelException(ErrorCode.INVALID_REQUEST, "companyCnpj and companyName go together");
            }
            return new Company(companyCnpj, companyName);
        }
        if (!filled(passport) || !filled(country)) {
            throw new HotelException(ErrorCode.INVALID_REQUEST, "guestPassport and guestCountry go together");
        }
        return new Passport(passport, country);
    }

    private static boolean filled(String text) {
        return text != null && !text.isBlank();
    }
}
