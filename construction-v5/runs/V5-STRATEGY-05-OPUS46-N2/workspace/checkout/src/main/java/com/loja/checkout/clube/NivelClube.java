package com.loja.checkout.clube;

import java.math.BigDecimal;

public interface NivelClube {

    String codigo();

    BigDecimal taxaCashback();

    boolean freteGratis();

    boolean brinde(BigDecimal subtotalProdutos);
}
