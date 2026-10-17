package com.loja.orderservice.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "customers")
public class Customer {
    @Id
    private String cpf;
    private String email;
    private BigDecimal creditBalance;
    private Instant createdAt;

    public Customer() {}

    public Customer(String cpf, String email) {
        this.cpf = cpf;
        this.email = email;
        this.creditBalance = BigDecimal.ZERO;
        this.createdAt = Instant.now();
    }

    public String getCpf() { return cpf; }
    public void setCpf(String cpf) { this.cpf = cpf; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public BigDecimal getCreditBalance() { return creditBalance; }
    public void setCreditBalance(BigDecimal creditBalance) { this.creditBalance = creditBalance; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
