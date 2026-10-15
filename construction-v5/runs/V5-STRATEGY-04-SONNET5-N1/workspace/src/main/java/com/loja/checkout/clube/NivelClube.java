package com.loja.checkout.clube;

import java.math.BigDecimal;

/**
 * Um nível do clube da loja. Cada nível tem seu próprio conjunto de
 * vantagens.
 */
public interface NivelClube {

    String nome();

    BigDecimal credito(BigDecimal subtotalProdutos);

    boolean freteGratis();

    boolean brinde(BigDecimal subtotalProdutos);
}
