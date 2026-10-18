package com.loja.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "customers")
public class Customer {
    @Id
    private String cpf;

    private BigDecimal creditBalance;

    @Version
    private Long version;

    protected Customer() {}

    public Customer(String cpf) {
        this.cpf = cpf;
        this.creditBalance = BigDecimal.ZERO.setScale(2);
    }

    public String getCpf() {
        return cpf;
    }

    public Money getCreditBalance() {
        return new Money(creditBalance);
    }

    public void addCredit(Money amount) {
        this.creditBalance = this.creditBalance.add(amount.value());
    }

    public void removeCredit(Money amount) {
        this.creditBalance = this.creditBalance.subtract(amount.value());
    }
}
