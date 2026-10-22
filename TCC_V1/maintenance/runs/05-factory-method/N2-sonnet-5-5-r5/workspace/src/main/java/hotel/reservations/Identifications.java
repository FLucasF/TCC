package hotel.reservations;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/** Turns the identification fields into the one kind of identification they describe. */
public final class Identifications {

    /** Every kind of identification; a new kind is one more entry here. */
    private static final List<Function<IdentificationFields, Optional<Identification>>> KINDS =
            List.of(Cpf::from, Company::from, ForeignGuest::from);

    private Identifications() {
    }

    public static Identification from(IdentificationFields fields) {
        List<Identification> found = KINDS.stream()
                .map(kind -> kind.apply(fields))
                .flatMap(Optional::stream)
                .toList();
        if (found.size() != 1) {
            throw new HotelException(
                    ErrorCode.INVALID_REQUEST,
                    "identify the guest with exactly one of: guestCpf; companyCnpj and companyName; "
                            + "guestPassport and guestCountry");
        }
        return found.get(0);
    }

    public static IdentificationFields fieldsOf(Identification identification) {
        return new IdentificationFields(
                identification.cpf(),
                identification.companyCnpj(),
                identification.companyName(),
                identification.passport(),
                identification.country());
    }
}
