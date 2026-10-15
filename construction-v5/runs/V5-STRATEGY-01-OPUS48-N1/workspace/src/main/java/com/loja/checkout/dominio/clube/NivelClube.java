package com.loja.checkout.dominio.clube;

import java.math.BigDecimal;

/**
 * Nível do cliente no clube. Cada nível tem seu conjunto de vantagens — crédito
 * de volta, frete grátis, brinde — e cada conjunto mora só na sua implementação.
 * Um nível novo entra como classe, sem mexer nos outros nem no cálculo geral.
 *
 * O crédito é devolvido já arredondado para centavos.
 */
public interface NivelClube {

    String codigo();

    BigDecimal credito(BigDecimal subtotalProdutos);

    default boolean freteGratis() {
        return false;
    }

    default boolean brinde(BigDecimal subtotalProdutos) {
        return false;
    }
}
