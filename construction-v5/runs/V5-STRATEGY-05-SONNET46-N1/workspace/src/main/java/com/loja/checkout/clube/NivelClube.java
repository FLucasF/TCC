package com.loja.checkout.clube;

import java.math.BigDecimal;

public interface NivelClube {
    String codigo();
    boolean temFreteGratis();
    BigDecimal calcularCredito(BigDecimal subtotalProdutos);
    boolean temBrinde(BigDecimal subtotalProdutos);
}
