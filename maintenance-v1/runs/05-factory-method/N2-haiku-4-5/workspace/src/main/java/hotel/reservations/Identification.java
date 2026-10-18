package hotel.reservations;

public sealed interface Identification {
    record CpfIdentification(String cpf) implements Identification {}

    record CompanyIdentification(String cnpj, String companyName) implements Identification {}

    record PassportIdentification(String passport, String country) implements Identification {}
}
