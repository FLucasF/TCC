package hotel.reservations;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Every way a guest can be identified. A new way is a new {@link Identification} and one line in {@link #KINDS}.
 */
public final class Identifications {

    /** Each kind answers with its identification when the fields it owns are given, and with nothing when none is. */
    private static final List<Function<IdentificationFields, Optional<Identification>>> KINDS = List.of(
            CpfIdentification::from, CompanyIdentification::from, PassportIdentification::from);

    private Identifications() {
    }

    /** The identification the fields describe: exactly one kind, with all of its fields. */
    public static Identification from(IdentificationFields fields) {
        List<Identification> given = KINDS.stream()
                .map(kind -> kind.apply(fields))
                .flatMap(Optional::stream)
                .toList();
        if (given.size() != 1) {
            throw new HotelException(ErrorCode.INVALID_REQUEST,
                    "identify the guest with exactly one of: guestCpf; companyCnpj and companyName; "
                            + "guestPassport and guestCountry");
        }
        return given.get(0);
    }

    static boolean whenGiven(String field) {
        return field != null && !field.isBlank();
    }

    /** Whether any of the fields is given; a kind that has some but not all of its fields is not valid. */
    static Optional<Identification> ifAny(Identification identification, String... ownFields) {
        long given = Stream.of(ownFields).filter(Identifications::whenGiven).count();
        if (given == 0) {
            return Optional.empty();
        }
        if (given < ownFields.length) {
            throw new HotelException(ErrorCode.INVALID_REQUEST, "the identification of the guest is incomplete");
        }
        return Optional.of(identification);
    }
}
