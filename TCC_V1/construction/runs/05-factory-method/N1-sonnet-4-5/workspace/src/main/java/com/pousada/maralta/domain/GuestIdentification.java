package com.pousada.maralta.domain;

public sealed interface GuestIdentification permits CpfIdentification, CnpjIdentification, PassportIdentification {
    <T> T accept(GuestIdentificationVisitor<T> visitor);
}
