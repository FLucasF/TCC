package com.loja.checkout.domain.clube;

import java.math.BigDecimal;

public interface NivelClube {

    String codigo();

    BigDecimal percentualCashback();

    boolean freteGratis();

    boolean temBrinde(BigDecimal subtotalProdutos);
}
