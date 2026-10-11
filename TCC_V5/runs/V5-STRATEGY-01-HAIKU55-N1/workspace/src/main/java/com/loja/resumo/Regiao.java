package com.loja.resumo;

import java.math.BigDecimal;

public enum Regiao implements Codigado {

    SUDESTE(new BigDecimal("0.010")),
    SUL(new BigDecimal("0.010")),
    CENTRO_OESTE(new BigDecimal("0.015")),
    NORTE(new BigDecimal("0.025")),
    NORDESTE(new BigDecimal("0.020"));

    private final BigDecimal taxaSeguro;

    Regiao(BigDecimal taxaSeguro) {
        this.taxaSeguro = taxaSeguro;
    }

    public BigDecimal taxaSeguro() {
        return taxaSeguro;
    }

    @Override
    public String codigo() {
        return name();
    }
}
