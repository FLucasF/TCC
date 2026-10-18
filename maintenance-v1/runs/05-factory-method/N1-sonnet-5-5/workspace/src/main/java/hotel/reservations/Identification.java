package hotel.reservations;

/** How the guest is identified on the reservation: who the document is issued to. */
public sealed interface Identification {

    /** A Brazilian person, identified by CPF. */
    record Cpf(String number) implements Identification {
    }

    /** A company booking a room for an employee, identified by CNPJ. */
    record Company(String cnpj, String name) implements Identification {
    }

    /** A foreign guest, identified by passport and country. */
    record Foreigner(String passport, String country) implements Identification {
    }
}
