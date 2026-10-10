package com.loja.checkout.regiao;

import java.math.BigDecimal;

public enum Regiao {

    SUDESTE(new BigDecimal("0.01")),
    SUL(new BigDecimal("0.01")),
    CENTRO_OESTE(new BigDecimal("0.015")),
    NORTE(new BigDecimal("0.025")),
    NORDESTE(new BigDecimal("0.02"));

    private final BigDecimal taxaSeguro;

    Regiao(BigDecimal taxaSeguro) {
        this.taxaSeguro = taxaSeguro;
    }

    public BigDecimal taxaSeguro() {
        return taxaSeguro;
    }

    public static Regiao buscar(String codigo) {
        if (codigo == null) return null;
        try {
            return valueOf(codigo);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
