package com.loja.dominio;

import java.math.BigDecimal;

public class Cliente {

    private final String cpf;
    private String email;
    private BigDecimal saldoCredito;

    public Cliente(String cpf, String email, BigDecimal saldoCredito) {
        this.cpf = cpf;
        this.email = email;
        this.saldoCredito = saldoCredito;
    }

    public String cpf() { return cpf; }
    public String email() { return email; }
    public BigDecimal saldoCredito() { return saldoCredito; }

    public void somarCredito(BigDecimal valor) {
        saldoCredito = Dinheiro.arredondar(saldoCredito.add(valor));
    }

    public void subtrairCredito(BigDecimal valor) {
        saldoCredito = Dinheiro.arredondar(saldoCredito.subtract(valor));
    }
}
