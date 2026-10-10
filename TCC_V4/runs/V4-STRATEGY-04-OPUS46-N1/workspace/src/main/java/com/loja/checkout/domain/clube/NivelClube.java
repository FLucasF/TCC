package com.loja.checkout.domain.clube;

import java.math.BigDecimal;

public interface NivelClube {

    String codigo();

    boolean freteGratis();

    BigDecimal calcularCredito(BigDecimal subtotalProdutos);

    boolean temBrinde(BigDecimal subtotalProdutos);
}
