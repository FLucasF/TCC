package com.pousada.domain;

public sealed interface GuestIdentification
    permits CpfIdentification, CnpjIdentification, PassportIdentification {
}
