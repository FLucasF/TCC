package com.pousada.domain;

public record CnpjIdentification(String cnpj, String companyName) implements GuestIdentification {
}
