package br.com.pousada.domain;

public record CnpjIdentification(String cnpj, String companyName) implements GuestIdentification {
    @Override
    public String getType() {
        return "CNPJ";
    }
}
