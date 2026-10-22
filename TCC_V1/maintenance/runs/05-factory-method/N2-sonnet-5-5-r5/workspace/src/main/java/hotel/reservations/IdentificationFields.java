package hotel.reservations;

/** The identification fields as they arrive (from the website) or are stored; the ones not used are null. */
public record IdentificationFields(String cpf, String cnpj, String companyName, String passport, String country) {

    static boolean isPresent(String field) {
        return field != null && !field.isBlank();
    }
}
