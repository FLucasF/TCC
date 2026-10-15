package com.loja.checkout.dominio.clube;

import com.loja.checkout.dominio.Codificavel;
import com.loja.checkout.dominio.Dinheiro;
import java.math.BigDecimal;

public interface NivelClube extends Codificavel {

    /** Crédito para a próxima compra, sobre o valor dos produtos. */
    default BigDecimal credito(BigDecimal subtotalProdutos) {
        return Dinheiro.ZERO;
    }

    default boolean isentaFrete() {
        return false;
    }

    default boolean temBrinde(BigDecimal subtotalProdutos) {
        return false;
    }
}
