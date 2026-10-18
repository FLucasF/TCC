package com.shop.dto;

public class CustomerInfo {
    private String cpf;
    private String email;

    public CustomerInfo() {}

    public CustomerInfo(String cpf, String email) {
        this.cpf = cpf;
        this.email = email;
    }

    public String getCpf() { return cpf; }
    public void setCpf(String cpf) { this.cpf = cpf; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
