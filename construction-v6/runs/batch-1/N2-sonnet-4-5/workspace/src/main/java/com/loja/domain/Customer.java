package com.loja.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "customers")
public class Customer {
    @Id
    private String cpf;

    private String email;

    @Column(name = "credit_balance")
    private BigDecimal creditBalance;

    @Column(name = "club_tier")
    private String clubTier;

    public Customer() {}

    public Customer(String cpf, String email, String clubTier) {
        this.cpf = cpf;
        this.email = email;
        this.creditBalance = BigDecimal.ZERO;
        this.clubTier = clubTier;
    }

    public String getCpf() { return cpf; }
    public void setCpf(String cpf) { this.cpf = cpf; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public BigDecimal getCreditBalance() { return creditBalance; }
    public void setCreditBalance(BigDecimal creditBalance) { this.creditBalance = creditBalance; }

    public String getClubTier() { return clubTier; }
    public void setClubTier(String clubTier) { this.clubTier = clubTier; }
}
