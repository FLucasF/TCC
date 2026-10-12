package com.loja.checkout.dominio.clube;

import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.Identificavel;

import java.math.BigDecimal;

/**
 * Um nivel do clube da loja. Cada nivel novo entra como uma implementacao
 * propria, com o seu conjunto de vantagens.
 */
public interface NivelClube extends Identificavel {

    /** Credito para a proxima compra, sobre o valor dos produtos. */
    default BigDecimal credito(BigDecimal subtotalProdutos) {
        return Dinheiro.ZERO;
    }

    /** Quanto o cliente paga de frete, a partir do frete da modalidade escolhida. */
    default BigDecimal freteCobrado(BigDecimal freteDaModalidade) {
        return freteDaModalidade;
    }

    default boolean brinde(BigDecimal subtotalProdutos) {
        return false;
    }
}
