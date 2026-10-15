package com.loja.checkout.dominio.clube;

import com.loja.checkout.dominio.Codificado;

import java.math.BigDecimal;

/**
 * Um nível do clube da loja. Cada nível tem seu conjunto de vantagens (crédito
 * para a próxima compra, isenção de frete, brinde), então cada um mora na sua
 * própria classe. Níveis novos entram só adicionando mais uma implementação.
 */
public interface NivelClube extends Codificado {

    /** Crédito para a próxima compra, calculado sobre o subtotal dos produtos. */
    default BigDecimal credito(BigDecimal subtotalProdutos) {
        return BigDecimal.ZERO;
    }

    /** Se o nível nunca paga frete. */
    default boolean isentaFrete() {
        return false;
    }

    /** Se o pedido ganha brinde, conforme o subtotal dos produtos. */
    default boolean temBrinde(BigDecimal subtotalProdutos) {
        return false;
    }
}
