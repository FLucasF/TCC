package hotel.reservations;

/** How the guest is identified on the reservation and on the document: exactly one of these. */
public sealed interface GuestIdentity {

    /** A Brazilian individual. */
    record Cpf(String cpf) implements GuestIdentity {
    }

    /** A company booking the room for the guest, who gets the invoice with the company's data. */
    record Company(String cnpj, String companyName) implements GuestIdentity {
    }

    /** A foreign guest. */
    record Passport(String number, String country) implements GuestIdentity {
    }
}
