package com.pousadamarealta.domain;

public sealed interface GuestIdentification
    permits GuestIdentification.IndividualGuest,
            GuestIdentification.CorporateGuest,
            GuestIdentification.ForeignGuest {

    record IndividualGuest(String cpf) implements GuestIdentification {}

    record CorporateGuest(String cnpj, String companyName) implements GuestIdentification {}

    record ForeignGuest(String passport, String country) implements GuestIdentification {}
}
