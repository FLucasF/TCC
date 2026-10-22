package hotel.reservations;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

/** The one place that turns the identification fields (as sent by the site or saved in the database) into a kind. */
public final class Identifications {

    private Identifications() {
    }

    /** Exactly one kind may have fields filled in, and it must have all of them. */
    public static Identification of(
            String cpf, String companyCnpj, String companyName, String passport, String country) {
        List<Candidate> candidates = List.of(
                new Candidate(() -> new CpfIdentification(cpf), cpf),
                new Candidate(() -> new CompanyIdentification(companyCnpj, companyName), companyCnpj, companyName),
                new Candidate(() -> new PassportIdentification(passport, country), passport, country));
        List<Candidate> touched = candidates.stream().filter(Candidate::touched).toList();
        if (touched.size() != 1 || !touched.get(0).complete()) {
            throw new HotelException(ErrorCode.INVALID_REQUEST,
                    "identify the guest with exactly one of: guestCpf; companyCnpj and companyName; "
                            + "guestPassport and guestCountry");
        }
        return touched.get(0).kind().get();
    }

    private record Candidate(Supplier<Identification> kind, String... fields) {

        boolean touched() {
            return Arrays.stream(fields).anyMatch(field -> !isBlank(field));
        }

        boolean complete() {
            return Arrays.stream(fields).noneMatch(Identifications::isBlank);
        }
    }

    private static boolean isBlank(String text) {
        return text == null || text.isBlank();
    }
}
