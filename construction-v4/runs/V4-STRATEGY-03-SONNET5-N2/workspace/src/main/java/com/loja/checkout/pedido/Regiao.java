package com.loja.checkout.pedido;

import java.math.BigDecimal;

import static java.math.RoundingMode.HALF_EVEN;

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
        return subtotalProdutos.multiply(percentualSeguro).setScale(2, HALF_EVEN);
    }
}
