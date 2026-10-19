package com.pousada.domain.model;

public class Guest {
    private final String name;
    private final String email;
    private final String cpf;

    public Guest(String name, String email, String cpf) {
        this.name = name;
        this.email = email;
        this.cpf = cpf;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getCpf() {
        return cpf;
    }
}
