package br.com.loja.checkout.clube;

import java.math.BigDecimal;

/**
 * Um nível do clube da loja e suas vantagens. Para criar um nível novo, crie uma classe
 * {@code @Component} que implemente esta interface.
 */
public interface NivelClube {

    String codigo();

    /** Percentual dos produtos devolvido em crédito para a próxima compra (ex.: 0.02 para 2%). */
    default BigDecimal percentualCredito() {
        return BigDecimal.ZERO;
    }

    /** Se o cliente deste nível não paga frete. */
    default boolean isentoDeFrete() {
        return false;
    }

    /** Se o pedido ganha brinde, a partir do valor dos produtos. */
    default boolean ganhaBrinde(BigDecimal subtotalProdutos) {
        return false;
    }
}
