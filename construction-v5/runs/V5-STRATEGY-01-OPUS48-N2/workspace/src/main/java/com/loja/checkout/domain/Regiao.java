package com.loja.checkout.domain;

import com.loja.checkout.dinheiro.Dinheiro;

import java.math.BigDecimal;

/**
 * Região do cliente, usada no seguro. O enunciado é explícito: "é só a
 * porcentagem que muda, a conta é a mesma em todas". Então aqui não há
 * comportamento por caso — só o percentual, e uma fórmula única.
 */
public enum Regiao {

    SUDESTE("0.01"),
    SUL("0.01"),
    CENTRO_OESTE("0.015"),
    NORTE("0.025"),
    NORDESTE("0.02");

    private final BigDecimal percentual;

    Regiao(String percentual) {
        this.percentual = new BigDecimal(percentual);
    }

    /** Seguro: percentual da região sobre os produtos, já em centavos. */
    public BigDecimal seguro(BigDecimal subtotalProdutos) {
        return Dinheiro.arredondar(percentual.multiply(subtotalProdutos));
    }
}
