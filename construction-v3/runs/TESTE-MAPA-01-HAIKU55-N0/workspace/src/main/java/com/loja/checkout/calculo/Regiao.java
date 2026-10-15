package com.loja.checkout.calculo;

import java.math.BigDecimal;

public enum Regiao {

    SUDESTE(new BigDecimal("0.010")),
    SUL(new BigDecimal("0.010")),
    CENTRO_OESTE(new BigDecimal("0.015")),
    NORTE(new BigDecimal("0.025")),
    NORDESTE(new BigDecimal("0.020"));

    private final BigDecimal percentualSeguro;

    Regiao(BigDecimal percentualSeguro) {
        this.percentualSeguro = percentualSeguro;
    }

    public BigDecimal percentualSeguro() {
        return percentualSeguro;
    }
}
