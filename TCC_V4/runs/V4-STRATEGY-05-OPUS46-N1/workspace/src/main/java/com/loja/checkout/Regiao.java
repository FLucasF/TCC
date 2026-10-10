package com.loja.checkout;

import java.math.BigDecimal;
import java.math.RoundingMode;

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

    public BigDecimal calcularSeguro(BigDecimal subtotalProdutos) {
        return subtotalProdutos.multiply(percentualSeguro).setScale(2, RoundingMode.HALF_EVEN);
    }
}
