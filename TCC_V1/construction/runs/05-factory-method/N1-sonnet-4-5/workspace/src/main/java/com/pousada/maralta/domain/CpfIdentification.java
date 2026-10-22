package com.pousada.maralta.domain;

public record CpfIdentification(String cpf) implements GuestIdentification {
    @Override
    public <T> T accept(GuestIdentificationVisitor<T> visitor) {
        return visitor.visitCpf(this);
    }
}
