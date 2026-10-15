package com.loja.checkout.domain;

import static com.loja.checkout.domain.Dinheiro.centavos;

import java.math.BigDecimal;

/**
 * Região do cliente. O seguro é a mesma conta em todas — a porcentagem sobre
 * o valor dos produtos — e só a porcentagem muda. Por isso a porcentagem é um
 * dado da região, não um comportamento próprio de cada caso.
 */
public enum Regiao {

    SUDESTE("0.01"),
    SUL("0.01"),
    CENTRO_OESTE("0.015"),
    NORTE("0.025"),
    NORDESTE("0.02");

    private final BigDecimal percentualSeguro;

    Regiao(String percentualSeguro) {
        this.percentualSeguro = new BigDecimal(percentualSeguro);
    }

    public BigDecimal seguro(BigDecimal subtotalProdutos) {
        return centavos(subtotalProdutos.multiply(percentualSeguro));
    }
}
