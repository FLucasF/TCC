package hotel.reservations;

/**
 * How a reservation is identified, which also decides the document it gets: a Brazilian guest by CPF, a
 * company by CNPJ and company name, a foreign guest by passport and country.
 */
public sealed interface Identification {

    record Cpf(String number) implements Identification {
    }

    record Company(String cnpj, String name) implements Identification {
    }

    record Foreign(String passport, String country) implements Identification {
    }
}
