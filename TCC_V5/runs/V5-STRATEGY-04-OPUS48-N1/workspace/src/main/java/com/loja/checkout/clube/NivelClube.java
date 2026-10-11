package com.loja.checkout.clube;

import java.math.BigDecimal;

/**
 * Um nível do clube. A loja estuda criar mais níveis, cada um com seu conjunto
 * de vantagens: cada nível é uma classe própria. As três vantagens possíveis
 * hoje são crédito para a próxima compra, não pagar frete e ganhar brinde.
 */
public interface NivelClube {

    String codigo();

    /** Crédito guardado para a próxima compra, sobre o valor dos produtos. */
    default BigDecimal credito(BigDecimal subtotalProdutos) {
        return BigDecimal.ZERO;
    }

    /** Ajusta o frete do pedido (ex.: OURO não paga frete, então zera). */
    default BigDecimal ajustarFrete(BigDecimal frete) {
        return frete;
    }

    /** Se o pedido ganha brinde. */
    default boolean brinde(BigDecimal subtotalProdutos) {
        return false;
    }
}
