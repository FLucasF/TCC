package hotel.web;

import hotel.reservations.ErrorCode;
import hotel.reservations.HotelException;
import hotel.reservations.Identification;
import java.util.List;
import java.util.function.Function;

/** Reads how the guest is identified from the request: exactly one kind, with all of its fields. */
final class IdentificationReader {

    /** One way of identifying a guest: the values its fields have in the request, and what they build. */
    private record Kind(List<String> values, Function<List<String>, Identification> build) {

        boolean isAbsent() {
            return values.stream().allMatch(IdentificationReader::isBlank);
        }

        boolean isComplete() {
            return values.stream().noneMatch(IdentificationReader::isBlank);
        }
    }

    private IdentificationReader() {
    }

    static Identification read(CreateReservationRequest body) {
        List<Kind> kinds = List.of(
                new Kind(List.of(text(body.guestCpf())),
                        values -> new Identification.Cpf(values.get(0))),
                new Kind(List.of(text(body.companyCnpj()), text(body.companyName())),
                        values -> new Identification.Company(values.get(0), values.get(1))),
                new Kind(List.of(text(body.guestPassport()), text(body.guestCountry())),
                        values -> new Identification.Passport(values.get(0), values.get(1))));

        List<Kind> given = kinds.stream().filter(kind -> !kind.isAbsent()).toList();
        if (given.size() != 1 || !given.get(0).isComplete()) {
            throw new HotelException(
                    ErrorCode.INVALID_REQUEST,
                    "identify the guest with exactly one of: guestCpf, companyCnpj with companyName, "
                            + "or guestPassport with guestCountry");
        }
        return given.get(0).build().apply(given.get(0).values());
    }

    private static String text(String value) {
        return value == null ? "" : value;
    }

    private static boolean isBlank(String value) {
        return value.isBlank();
    }
}
