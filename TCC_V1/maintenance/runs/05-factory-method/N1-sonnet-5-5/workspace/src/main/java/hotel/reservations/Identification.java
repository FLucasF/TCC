package hotel.reservations;

/**
 * How a guest is identified on the reservation: by CPF, by the company paying (CNPJ and company name), or
 * by passport and country. The accessors of the data a kind does not have answer null.
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

    record PersonalId(String cpf) implements Identification {
    }

    record Company(String companyCnpj, String companyName) implements Identification {
    }

    record Foreigner(String passport, String country) implements Identification {
    }

    /** Exactly one of the ways, with all of its data; anything else is an invalid request. */
    static Identification of(String cpf, String cnpj, String companyName, String passport, String country) {
        boolean personal = !isBlank(cpf);
        boolean company = !isBlank(cnpj) || !isBlank(companyName);
        boolean foreigner = !isBlank(passport) || !isBlank(country);
        if ((personal ? 1 : 0) + (company ? 1 : 0) + (foreigner ? 1 : 0) != 1) {
            throw invalid("give the CPF, or the CNPJ with the company name, or the passport with the country");
        }
        if (personal) {
            return new PersonalId(cpf);
        }
        if (company) {
            if (isBlank(cnpj) || isBlank(companyName)) {
                throw invalid("a company needs both the CNPJ and the company name");
            }
            return new Company(cnpj, companyName);
        }
        if (isBlank(passport) || isBlank(country)) {
            throw invalid("a foreign guest needs both the passport and the country");
        }
        return new Foreigner(passport, country);
    }

    private static HotelException invalid(String message) {
        return new HotelException(ErrorCode.INVALID_REQUEST, message);
    }

    private static boolean isBlank(String text) {
        return text == null || text.isBlank();
    }
}
