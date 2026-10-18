package com.loja.orderservice.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "customers")
public class Customer {

    @Id
    private String cpf;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal creditBalance;

    public Customer() {
        this.creditBalance = BigDecimal.ZERO;
    }

    public Customer(String cpf) {
        this.cpf = cpf;
        this.creditBalance = BigDecimal.ZERO;
    }

    public String getCpf() {
        return cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }

    public BigDecimal getCreditBalance() {
        return creditBalance;
    }

    public void setCreditBalance(BigDecimal creditBalance) {
        this.creditBalance = creditBalance;
    }

    public void addCredit(BigDecimal amount) {
        this.creditBalance = this.creditBalance.add(amount);
    }

    public void subtractCredit(BigDecimal amount) {
        this.creditBalance = this.creditBalance.subtract(amount);
    }
}
