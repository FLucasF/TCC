package com.loja.checkout.clube;

import com.loja.checkout.dominio.Identificavel;

import java.math.BigDecimal;

/**
 * Um nível do clube da loja. Cada nível novo é uma implementação desta interface,
 * com o seu conjunto de vantagens.
 */
public interface NivelClube extends Identificavel {

    /** Fração dos produtos que volta em crédito (ex.: 0.02 para 2%). */
    default BigDecimal percentualCredito() {
        return BigDecimal.ZERO;
    }

    /** Se o nível isenta o frete sempre. */
    default boolean freteGratis() {
        return false;
    }

    /** Se o pedido ganha brinde. */
    default boolean temBrinde(BigDecimal subtotalProdutos) {
        return false;
    }
}
