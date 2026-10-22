package com.pousada.maralta.domain;

public interface GuestIdentificationVisitor<T> {
    T visitCpf(CpfIdentification cpf);
    T visitCnpj(CnpjIdentification cnpj);
    T visitPassport(PassportIdentification passport);
}
