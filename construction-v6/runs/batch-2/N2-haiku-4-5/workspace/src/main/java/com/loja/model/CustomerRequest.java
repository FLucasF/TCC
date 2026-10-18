package com.loja.model;

public class CustomerRequest {
    public String cpf;
    public String email;

    public CustomerRequest() {}

    public CustomerRequest(String cpf, String email) {
        this.cpf = cpf;
        this.email = email;
    }
}
