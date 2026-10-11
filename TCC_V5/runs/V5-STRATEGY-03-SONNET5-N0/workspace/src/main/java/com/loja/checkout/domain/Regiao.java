package com.loja.checkout.domain;

import java.math.BigDecimal;

/**
 * Percentual cobrado pela seguradora contra extravio e roubo, por regiao do cliente.
 * A conta e sempre a mesma: percentual sobre o valor dos produtos (sem desconto, sem frete).
 */
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
        return Dinheiro.arredondar(subtotalProdutos.multiply(percentualSeguro));
    }
}
