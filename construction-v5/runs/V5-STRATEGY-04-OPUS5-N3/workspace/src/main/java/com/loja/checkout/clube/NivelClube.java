package com.loja.checkout.clube;

import com.loja.checkout.dominio.Dinheiro;

import java.math.BigDecimal;

/** Um nível do clube: o conjunto de vantagens que o cliente recebe. */
public interface NivelClube {

    String codigo();

    /** Crédito para a próxima compra, sobre o valor dos produtos. */
    default BigDecimal credito(BigDecimal subtotalProdutos) {
        return Dinheiro.ZERO;
    }

    default boolean isentaFrete() {
        return false;
    }

    default boolean brinde(BigDecimal subtotalProdutos) {
        return false;
    }
}
