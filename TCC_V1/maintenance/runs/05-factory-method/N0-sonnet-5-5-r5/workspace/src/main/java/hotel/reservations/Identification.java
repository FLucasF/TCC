package hotel.reservations;

/** How the guest is identified on the reservation, which decides the document they get. */
public sealed interface Identification {

    /** A Brazilian guest: the regular receipt. */
    record Cpf(String number) implements Identification {
    }

    /** A company booking the room for the guest: the lodging invoice. */
    record Company(String cnpj, String name) implements Identification {
    }

    /** A foreign guest: the booking receipt, in English. */
    record Passport(String number, String country) implements Identification {
    }
}
