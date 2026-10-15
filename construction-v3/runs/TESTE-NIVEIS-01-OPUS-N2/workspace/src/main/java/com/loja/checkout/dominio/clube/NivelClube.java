package com.loja.checkout.dominio.clube;

import com.loja.checkout.dominio.Catalogavel;
import com.loja.checkout.dominio.Dinheiro;
import java.math.BigDecimal;

/**
 * Um nivel do clube da loja. Cada nivel novo e uma implementacao desta
 * interface, com seu conjunto de vantagens.
 */
public interface NivelClube extends Catalogavel {

    /** Credito para a proxima compra, sobre o valor dos produtos. */
    default BigDecimal credito(BigDecimal subtotalProdutos) {
        return Dinheiro.ZERO;
    }

    default boolean freteGratis() {
        return false;
    }

    default boolean brinde(BigDecimal subtotalProdutos) {
        return false;
    }
}
