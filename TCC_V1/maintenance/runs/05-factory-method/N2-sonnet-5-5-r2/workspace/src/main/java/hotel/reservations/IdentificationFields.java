package hotel.reservations;

/** The fields that identify a guest, as they come in the request and sit in the database; the unused ones are null. */
public record IdentificationFields(
        String cpf, String companyCnpj, String companyName, String guestPassport, String guestCountry) {
}
