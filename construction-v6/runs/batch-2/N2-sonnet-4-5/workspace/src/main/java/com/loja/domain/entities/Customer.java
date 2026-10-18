package com.loja.domain.entities;

public class Customer {
    private final String cpf;
    private Money creditBalance;

    public Customer(String cpf) {
        this.cpf = cpf;
        this.creditBalance = Money.zero();
    }

    public Customer(String cpf, Money creditBalance) {
        this.cpf = cpf;
        this.creditBalance = creditBalance;
    }

    public String getCpf() {
        return cpf;
    }

    public Money getCreditBalance() {
        return creditBalance;
    }

    public void addCredit(Money amount) {
        this.creditBalance = this.creditBalance.add(amount);
    }

    public void subtractCredit(Money amount) {
        this.creditBalance = this.creditBalance.subtract(amount);
    }
}
