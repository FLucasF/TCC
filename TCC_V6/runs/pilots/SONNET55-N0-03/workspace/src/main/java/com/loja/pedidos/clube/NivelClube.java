package com.loja.pedidos.clube;

import java.math.BigDecimal;

/** Um nível do clube e suas vantagens. Novo nível = novo bean. */
public interface NivelClube {

    String codigo();

    /** Fração do valor dos produtos devolvida como crédito (0.05 = 5%). */
    BigDecimal fracaoCredito();

    boolean isentaFrete();

    boolean brinde(BigDecimal subtotalProdutos);
}
