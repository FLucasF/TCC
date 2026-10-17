package com.loja.orderservice.dto;

import java.math.BigDecimal;

public class CustomerResponseDTO {
    private String cpf;
    private BigDecimal creditBalance;

    public CustomerResponseDTO() {}

    public CustomerResponseDTO(String cpf, BigDecimal creditBalance) {
        this.cpf = cpf;
        this.creditBalance = creditBalance;
    }

    public String getCpf() { return cpf; }
    public void setCpf(String cpf) { this.cpf = cpf; }

    public BigDecimal getCreditBalance() { return creditBalance; }
    public void setCreditBalance(BigDecimal creditBalance) { this.creditBalance = creditBalance; }
}
