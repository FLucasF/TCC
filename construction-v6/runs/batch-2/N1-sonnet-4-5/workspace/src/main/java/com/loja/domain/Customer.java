package com.loja.domain;

import java.math.BigDecimal;

public class Customer {
    private String cpf;
    private String email;
    private BigDecimal creditBalance;

    public Customer() {
    }

    public Customer(String cpf, String email, BigDecimal creditBalance) {
        this.cpf = cpf;
        this.email = email;
        this.creditBalance = creditBalance;
    }

    public String getCpf() {
        return cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public BigDecimal getCreditBalance() {
        return creditBalance;
    }

    public void setCreditBalance(BigDecimal creditBalance) {
        this.creditBalance = creditBalance;
    }
}
