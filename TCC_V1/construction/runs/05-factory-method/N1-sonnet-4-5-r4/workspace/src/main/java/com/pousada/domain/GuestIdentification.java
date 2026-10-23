package com.pousada.domain;

public sealed interface GuestIdentification {
    record Cpf(String cpf) implements GuestIdentification {}
    record Cnpj(String cnpj, String companyName) implements GuestIdentification {}
    record Passport(String passport, String country) implements GuestIdentification {}
}
