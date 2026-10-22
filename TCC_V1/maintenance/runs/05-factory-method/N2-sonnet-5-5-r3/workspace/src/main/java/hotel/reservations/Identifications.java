package hotel.reservations;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

/** The one place that knows every kind of identification: how to read one from a request and from the database. */
public final class Identifications {

    /** The identification fields as they come in a request; any of them may be null. */
    public record Fields(String cpf, String companyCnpj, String companyName, String passport, String country) {
    }

    private static final List<Function<Fields, Optional<Identification>>> FROM_REQUEST = List.of(
            fields -> given(fields.cpf())
                    ? Optional.of(new CpfIdentification(fields.cpf())) : Optional.empty(),
            fields -> given(fields.companyCnpj(), fields.companyName())
                    ? Optional.of(new CompanyIdentification(fields.companyCnpj(), fields.companyName()))
                    : Optional.empty(),
            fields -> given(fields.passport(), fields.country())
                    ? Optional.of(new PassportIdentification(fields.passport(), fields.country()))
                    : Optional.empty());

    private static final Map<String, BiFunction<String, String, Identification>> FROM_DATABASE = Map.of(
            CpfIdentification.TYPE, (number, detail) -> new CpfIdentification(number),
            CompanyIdentification.TYPE, CompanyIdentification::new,
            PassportIdentification.TYPE, PassportIdentification::new);

    private Identifications() {
    }

    /** The identification the request carries: exactly one kind, with all of its fields. */
    public static Identification fromRequest(Fields fields) {
        List<Identification> found = FROM_REQUEST.stream()
                .map(kind -> kind.apply(fields))
                .flatMap(Optional::stream)
                .toList();
        if (found.size() != 1) {
            throw new HotelException(ErrorCode.INVALID_REQUEST,
                    "the guest needs exactly one identification: guestCpf, companyCnpj with companyName, "
                            + "or guestPassport with guestCountry");
        }
        return found.get(0);
    }

    static Identification fromDatabase(String type, String number, String detail) {
        return FROM_DATABASE.get(type).apply(number, detail);
    }

    /**
     * A kind is given when any of its fields is filled. If it is given but some field is missing, it is incomplete
     * and the request is rejected.
     */
    private static boolean given(String... values) {
        int filled = 0;
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                filled++;
            }
        }
        if (filled > 0 && filled < values.length) {
            throw new HotelException(ErrorCode.INVALID_REQUEST, "the identification of the guest is incomplete");
        }
        return filled > 0;
    }
}
