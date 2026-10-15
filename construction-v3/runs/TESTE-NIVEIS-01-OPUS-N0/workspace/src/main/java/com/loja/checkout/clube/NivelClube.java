package com.loja.checkout.clube;

import com.loja.checkout.comum.Identificavel;
import java.math.BigDecimal;

/**
 * Um nivel do clube da loja. Para criar um nivel novo,
 * basta um bean novo com o conjunto de vantagens dele.
 */
public interface NivelClube extends Identificavel {

    /** Percentual dos produtos que volta em credito (em fracao, ex.: 0.02 para 2%). */
    default BigDecimal percentualCredito() {
        return BigDecimal.ZERO;
    }

    /** Se o nivel nunca paga frete. */
    default boolean freteGratis() {
        return false;
    }

    /** Se o pedido vai com brinde. */
    default boolean temBrinde(BigDecimal subtotalProdutos) {
        return false;
    }
}
