package com.loja.checkout.clube;

import java.math.BigDecimal;

public interface NivelClube {

    String codigo();

    BigDecimal percentualCredito();

    boolean freteGratis();

    boolean brinde(BigDecimal subtotalProdutos);
}
