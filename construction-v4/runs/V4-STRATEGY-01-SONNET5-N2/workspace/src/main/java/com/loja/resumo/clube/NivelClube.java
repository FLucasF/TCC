package com.loja.resumo.clube;

import java.math.BigDecimal;

public interface NivelClube {

    String codigo();

    BigDecimal percentualCredito();

    boolean isentoFrete();

    boolean concedeBrinde(BigDecimal subtotalProdutos);
}
