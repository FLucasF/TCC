package hotel.reservations;

/** Chooses the kind of identification from the fields a request or a stored reservation carries. */
public final class Identifications {

    private Identifications() {
    }

    /** Exactly one kind must be complete and nothing of the others may be present. */
    public static Identification of(
            String cpf, String companyCnpj, String companyName, String passport, String country) {
        boolean hasCpf = !isBlank(cpf);
        boolean hasCompany = !isBlank(companyCnpj) || !isBlank(companyName);
        boolean hasPassport = !isBlank(passport) || !isBlank(country);
        if (hasCpf && !hasCompany && !hasPassport) {
            return new CpfIdentification(cpf);
        }
        if (hasCompany && !hasCpf && !hasPassport && !isBlank(companyCnpj) && !isBlank(companyName)) {
            return new CompanyIdentification(companyCnpj, companyName);
        }
        if (hasPassport && !hasCpf && !hasCompany && !isBlank(passport) && !isBlank(country)) {
            return new PassportIdentification(passport, country);
        }
        throw new HotelException(
                ErrorCode.INVALID_REQUEST,
                "identify the guest by guestCpf, or by companyCnpj with companyName, or by guestPassport with guestCountry");
    }

    private static boolean isBlank(String text) {
        return text == null || text.isBlank();
    }
}
