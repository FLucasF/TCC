package com.pousadamarealta.domain;

public class CpfIdentification implements GuestIdentification {
    private final String cpf;

    public CpfIdentification(String cpf) {
        this.cpf = cpf;
    }

    public String getCpf() {
        return cpf;
    }

    @Override
    public String formatForDocument() {
        return "CPF " + cpf;
    }
}
