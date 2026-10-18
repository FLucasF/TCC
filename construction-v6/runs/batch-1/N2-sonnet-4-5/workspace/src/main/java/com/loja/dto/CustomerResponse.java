package com.loja.dto;

import java.math.BigDecimal;

public class CustomerResponse {
    private String cpf;
    private BigDecimal creditBalance;

    public String getCpf() { return cpf; }
    public void setCpf(String cpf) { this.cpf = cpf; }

    public BigDecimal getCreditBalance() { return creditBalance; }
    public void setCreditBalance(BigDecimal creditBalance) { this.creditBalance = creditBalance; }
}
