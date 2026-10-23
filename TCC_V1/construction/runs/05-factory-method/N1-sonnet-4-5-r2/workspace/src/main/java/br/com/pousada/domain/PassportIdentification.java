package br.com.pousada.domain;

public record PassportIdentification(String passport, String country) implements GuestIdentification {
    @Override
    public String getType() {
        return "PASSPORT";
    }
}
