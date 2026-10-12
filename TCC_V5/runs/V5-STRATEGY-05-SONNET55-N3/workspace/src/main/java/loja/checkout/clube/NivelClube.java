package loja.checkout.clube;

import java.math.BigDecimal;

import loja.checkout.Codificado;
import loja.checkout.Dinheiro;

public interface NivelClube extends Codificado {

    /** Crédito para a próxima compra, sobre o valor dos produtos. */
    default BigDecimal credito(BigDecimal subtotalProdutos) {
        return Dinheiro.ZERO;
    }

    default boolean isentoDeFrete() {
        return false;
    }

    default boolean brinde(BigDecimal subtotalProdutos) {
        return false;
    }
}
