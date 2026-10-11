package com.loja.checkout.domain;

import com.loja.checkout.service.CheckoutException;

import java.math.BigDecimal;

public enum Regiao {

    SUDESTE(new BigDecimal("0.01")),
    SUL(new BigDecimal("0.01")),
    CENTRO_OESTE(new BigDecimal("0.015")),
    NORTE(new BigDecimal("0.025")),
    NORDESTE(new BigDecimal("0.02"));

    private final BigDecimal percentualSeguro;

    Regiao(BigDecimal percentualSeguro) {
        this.percentualSeguro = percentualSeguro;
    }

    public BigDecimal percentualSeguro() {
        return percentualSeguro;
    }

    public static Regiao from(String codigo) {
        if (codigo == null) {
            throw new CheckoutException("REGIAO_INVALIDA");
        }
        try {
            return valueOf(codigo);
        } catch (IllegalArgumentException e) {
            throw new CheckoutException("REGIAO_INVALIDA");
        }
    }
}
