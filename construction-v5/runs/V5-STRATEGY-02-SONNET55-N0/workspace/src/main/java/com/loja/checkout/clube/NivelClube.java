package com.loja.checkout.clube;

import java.math.BigDecimal;

/** Novo nível = nova classe @Component implementando esta interface. */
public interface NivelClube {
    String codigo();

    /** Fração do valor dos produtos devolvida como crédito (ex.: 0.02). */
    default BigDecimal taxaCredito() {
        return BigDecimal.ZERO;
    }

    default boolean freteGratis() {
        return false;
    }

    default boolean brinde(BigDecimal subtotalProdutos) {
        return false;
    }
}
