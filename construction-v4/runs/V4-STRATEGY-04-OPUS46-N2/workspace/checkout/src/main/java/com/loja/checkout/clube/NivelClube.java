package com.loja.checkout.clube;

import java.math.BigDecimal;

public interface NivelClube {

    String codigo();

    BigDecimal calcularCredito(BigDecimal subtotalProdutos);

    boolean isFreteGratis();

    boolean temBrinde(BigDecimal subtotalProdutos);
}
