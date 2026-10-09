package com.loja.checkout.clube;

import java.math.BigDecimal;

/**
 * Cada nivel do clube define seu proprio conjunto de vantagens. Novos niveis
 * sao adicionados implementando esta interface e registrando o bean no Spring.
 */
public interface ClubeStrategy {

    String codigo();

    boolean fretesGratis();

    BigDecimal percentualCredito();

    boolean temBrinde(BigDecimal subtotalProdutos);
}
