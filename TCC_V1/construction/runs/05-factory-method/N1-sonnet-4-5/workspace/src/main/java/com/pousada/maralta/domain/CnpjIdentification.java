package com.pousada.maralta.domain;

public record CnpjIdentification(String cnpj, String companyName) implements GuestIdentification {
    @Override
    public <T> T accept(GuestIdentificationVisitor<T> visitor) {
        return visitor.visitCnpj(this);
    }
}
