package com.loja.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Customer {
    public String cpf;
    @JsonProperty("creditBalance")
    public Double creditBalance;

    public Customer() {}

    public Customer(String cpf, Double creditBalance) {
        this.cpf = cpf;
        this.creditBalance = creditBalance;
    }
}
