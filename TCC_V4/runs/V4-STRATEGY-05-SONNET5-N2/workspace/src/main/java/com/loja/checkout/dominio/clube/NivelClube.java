package com.loja.checkout.dominio.clube;

import java.math.BigDecimal;

public interface NivelClube {

    String codigo();

    BigDecimal credito(BigDecimal subtotalProdutos);

    boolean freteGratis();

    boolean brinde(BigDecimal subtotalProdutos);
}
