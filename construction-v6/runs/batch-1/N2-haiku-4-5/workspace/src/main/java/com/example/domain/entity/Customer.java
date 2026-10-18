package com.example.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "customers")
public class Customer {

    @Id
    @Column(length = 11)
    private String cpf;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal creditBalance;

    protected Customer() {
    }

    public Customer(String cpf) {
        this.cpf = cpf;
        this.creditBalance = BigDecimal.ZERO;
    }

    public String getCpf() {
        return cpf;
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
