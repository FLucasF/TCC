package com.loja.checkout.clube;

import com.loja.checkout.dominio.Codificavel;
import com.loja.checkout.dominio.Dinheiro;

import java.math.BigDecimal;

/** Um nível do clube da loja, com as vantagens que ele dá. */
public interface NivelClube extends Codificavel {

    default boolean isentaFrete() {
        return false;
    }

    default BigDecimal credito(BigDecimal subtotalProdutos) {
        return Dinheiro.ZERO;
    }

    default boolean brinde(BigDecimal subtotalProdutos) {
        return false;
    }
}
