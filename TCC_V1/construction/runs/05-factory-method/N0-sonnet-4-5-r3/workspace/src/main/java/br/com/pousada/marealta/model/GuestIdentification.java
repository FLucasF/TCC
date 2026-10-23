package br.com.pousada.marealta.model;

public sealed interface GuestIdentification permits
    GuestIdentification.Individual,
    GuestIdentification.Company,
    GuestIdentification.Foreign {

    record Individual(String cpf) implements GuestIdentification {}

    record Company(String cnpj, String companyName) implements GuestIdentification {}

    record Foreign(String passport, String country) implements GuestIdentification {}
}
