package com.example.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public class CustomerDto {
    @JsonProperty
    private String cpf;

    @JsonProperty("creditBalance")
    private BigDecimal creditBalance;

    public CustomerDto(String cpf, BigDecimal creditBalance) {
        this.cpf = cpf;
        this.creditBalance = creditBalance;
    }

    public String getCpf() {
        return cpf;
    }

    public BigDecimal getCreditBalance() {
        return creditBalance;
    }
}
