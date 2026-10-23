package br.com.pousada.domain;

public record CpfIdentification(String cpf) implements GuestIdentification {
    @Override
    public String getType() {
        return "CPF";
    }
}
