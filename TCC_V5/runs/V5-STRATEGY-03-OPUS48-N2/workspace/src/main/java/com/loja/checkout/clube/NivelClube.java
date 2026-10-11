package com.loja.checkout.clube;

import java.math.BigDecimal;

/**
 * Nivel do cliente no clube. Cada nivel tem seu conjunto de vantagens (credito,
 * frete gratis, brinde) e a loja estuda criar novos niveis. Cada nivel mora numa
 * classe propria; um nivel novo e uma nova implementacao.
 */
public interface NivelClube {

    String codigo();

    /** Credito para a proxima compra, sobre o valor dos produtos. */
    default BigDecimal credito(BigDecimal subtotalProdutos) {
        return BigDecimal.ZERO;
    }

    /** Se o nivel isenta o frete. */
    default boolean freteGratis() {
        return false;
    }

    /** Se acompanha brinde, de acordo com o valor dos produtos. */
    default boolean brinde(BigDecimal subtotalProdutos) {
        return false;
    }
}
