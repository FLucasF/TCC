package com.loja.checkout.domain.clube;

import com.loja.checkout.domain.Dinheiro;
import java.math.BigDecimal;

/**
 * Nível do cliente no clube. Cada nível tem seu conjunto de vantagens (crédito,
 * frete grátis, brinde), então cada um mora numa implementação própria. Entra
 * nível novo adicionando uma nova implementação.
 */
public interface NivelClube {

    String codigo();

    /** Crédito para a próxima compra, sobre o valor dos produtos. */
    default BigDecimal credito(BigDecimal subtotalProdutos) {
        return Dinheiro.centavos(subtotalProdutos.multiply(percentualCredito()));
    }

    /** Percentual de crédito do nível (zero por padrão). */
    default BigDecimal percentualCredito() {
        return BigDecimal.ZERO;
    }

    /** Se o nível nunca paga frete. Por padrão, paga. */
    default boolean freteGratis() {
        return false;
    }

    /** Se o pedido ganha brinde. Por padrão, não ganha. */
    default boolean brinde(BigDecimal subtotalProdutos) {
        return false;
    }
}
