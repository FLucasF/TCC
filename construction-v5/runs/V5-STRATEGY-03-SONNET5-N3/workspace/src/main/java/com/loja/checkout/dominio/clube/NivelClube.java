package com.loja.checkout.dominio.clube;

import java.math.BigDecimal;

public interface NivelClube {

    String codigo();

    BigDecimal percentualCredito();

    boolean freteGratis();

    boolean concedeBrinde(BigDecimal subtotalProdutos);
}
