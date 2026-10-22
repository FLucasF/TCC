package com.pousada.maralta.domain;

public record PassportIdentification(String passport, String country) implements GuestIdentification {
    @Override
    public <T> T accept(GuestIdentificationVisitor<T> visitor) {
        return visitor.visitPassport(this);
    }
}
